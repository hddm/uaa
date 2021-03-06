/*******************************************************************************
 *     Cloud Foundry 
 *     Copyright (c) [2009-2014] Pivotal Software, Inc. All Rights Reserved.
 *
 *     This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *     You may not use this product except in compliance with the License.
 *
 *     This product includes a number of subcomponents with
 *     separate copyright notices and license terms. Your use of these
 *     subcomponents is subject to the terms and conditions of the
 *     subcomponent's license, as noted in the LICENSE file.
 *******************************************************************************/
package org.cloudfoundry.identity.uaa.audit.event;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.audit.AuditEvent;
import org.cloudfoundry.identity.uaa.audit.AuditEventType;
import org.cloudfoundry.identity.uaa.oauth.approval.Approval;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.security.core.Authentication;

import java.io.IOException;

public class ApprovalModifiedEvent extends AbstractUaaEvent {
    private final Log logger = LogFactory.getLog(getClass());

    public ApprovalModifiedEvent(Object source, Authentication authentication) {
        super(source, authentication);
        if (!Approval.class.isAssignableFrom(source.getClass())) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Approval getSource() {
        return (Approval) super.getSource();
    }


    @Override
    public AuditEvent getAuditEvent() {
        Approval source = getSource();
        return createAuditRecord(source.getUserId(), AuditEventType.ApprovalModifiedEvent, getOrigin(getAuthentication()), getData(source));
    }

    private String getData(Approval source) {
        try {
            return new ObjectMapper().writeValueAsString(new ApprovalModifiedEventData(source));
        } catch (IOException e) {
            logger.error("error writing approval event data", e);
        }
        return null;
    }

    private static class ApprovalModifiedEventData {
        private String scope;
        private Approval.ApprovalStatus status;

        public ApprovalModifiedEventData(Approval approval) {
            scope = approval.getScope();
            status = approval.getStatus();
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public Approval.ApprovalStatus getStatus() {
            return status;
        }

        public void setStatus(Approval.ApprovalStatus status) {
            this.status = status;
        }
    }
}
