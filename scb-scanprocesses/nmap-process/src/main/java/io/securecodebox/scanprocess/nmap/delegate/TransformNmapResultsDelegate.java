/*
 *
 *  SecureCodeBox (SCB)
 *  Copyright 2015-2018 iteratec GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package io.securecodebox.scanprocess.nmap.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.securecodebox.constants.DefaultFields;
import io.securecodebox.model.execution.ScanProcessExecution;
import io.securecodebox.model.execution.ScanProcessExecutionFactory;
import io.securecodebox.model.findings.Finding;
import io.securecodebox.model.findings.OsiLayer;
import io.securecodebox.model.findings.Severity;
import io.securecodebox.scanprocess.nmap.constants.NmapFindingAttributes;
import io.securecodebox.scanprocess.nmap.model.Address;
import io.securecodebox.scanprocess.nmap.model.Host;
import io.securecodebox.scanprocess.nmap.model.NmapRawResult;
import io.securecodebox.scanprocess.nmap.model.Port;
import io.securecodebox.scanprocess.nmap.model.Ports;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.List;
import java.util.UUID;

/**
 * @author Rüdiger Heins - iteratec GmbH
 * @since 01.03.18
 */
@Component
public class TransformNmapResultsDelegate implements JavaDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(TransformNmapResultsDelegate.class);

    @Autowired
    ScanProcessExecutionFactory processExecutionFactory;

    @Autowired
    ObjectMapper objectMapper;

    DocumentBuilderFactory documentBuilderFactory;

    public TransformNmapResultsDelegate() {
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
    }

    public TransformNmapResultsDelegate(DocumentBuilderFactory documentBuilderFactory) {
        this.documentBuilderFactory = documentBuilderFactory;
    }

    private void clearFindings(ScanProcessExecution process) {
        if (!process.getFindings().isEmpty()) {
            LOG.debug("Clearing findings. The process had {}", process.getFindings().size());
            process.clearFindings();
        }
    }

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        ScanProcessExecution process = processExecutionFactory.get(delegateExecution);
        clearFindings(process);

        String rawFindingResult = process.getScanners().get(process.getScanners().size() - 1).getRawFindings();

        if (!StringUtils.isEmpty(rawFindingResult)) {

            List<String> findings = objectMapper.readValue(rawFindingResult,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

            findings.stream().forEach((findingXML) -> parseRawFindingAndAppend(process, findingXML));

            LOG.debug("Found {} findings", process.getFindings().size());

            // persist all generic result entries
            //new GenericReporter(delegateExecution).setGenericResultsVariable(findingsList)
        } else {
            LOG.warn("Couldn't find the process variable or its content is empty: {}",
                    DefaultFields.PROCESS_RAW_FINDINGS);
        }
    }

    private void parseRawFindingAndAppend(ScanProcessExecution process, String rawFindingResultXML) {
        final JAXBContext context;
        try {
            context = JAXBContext.newInstance(NmapRawResult.class);
            final Unmarshaller unmarshaller = context.createUnmarshaller();
            NmapRawResult rawResult = (NmapRawResult) unmarshaller.unmarshal(new StringReader(rawFindingResultXML));

            for (Host host : rawResult.getHosts()) {
                for (Ports ports : host.getPorts()) {
                    for (Port port : ports.getPort()) {
                        Finding finding = new Finding();
                        finding.setId(UUID.randomUUID());
                        finding.setCategory("Open Port");
                        finding.setName(String.format("Open %s Port", port.getService().getName()));
                        finding.setOsiLayer(OsiLayer.NETWORK);
                        finding.setDescription(String.format("Port %d is open using %s protocol.", port.getPortid(),
                                port.getProtocol()));
                        finding.setLocation(
                                port.getProtocol() + "://" + host.getIpAdress().orElse(new Address()).getAddr() + ":"
                                        + port.getPortid());
                        finding.setSeverity(Severity.INFORMATIONAL);
                        finding.addAttribute(NmapFindingAttributes.PORT, port.getPortid());
                        finding.addAttribute(NmapFindingAttributes.SERVICE, port.getService().getName());
                        finding.addAttribute(NmapFindingAttributes.PROTOCOL, port.getProtocol());
                        finding.addAttribute(NmapFindingAttributes.IP_ADDRESS,
                                host.getIpAdress().orElse(new Address()).getAddr());
                        finding.addAttribute(NmapFindingAttributes.MAC_ADDRESS,
                                host.getMacAdress().orElse(new Address()).getAddr());
                        finding.addAttribute(NmapFindingAttributes.STATE, port.getState().getState());
                        finding.addAttribute(NmapFindingAttributes.START, host.getStarttime());
                        finding.addAttribute(NmapFindingAttributes.END, host.getEndtime());
                        process.appendFinding(finding);
                        LOG.trace("Finding: {}", finding);
                    }
                }
            }
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

}
