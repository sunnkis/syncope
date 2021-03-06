/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.provisioning.java.data;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.to.AuditEntryTO;
import org.apache.syncope.core.persistence.api.entity.AuditEntry;
import org.apache.syncope.core.provisioning.api.data.AuditDataBinder;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.stereotype.Component;

@Component
public class AuditDataBinderImpl implements AuditDataBinder {

    @Override
    public AuditEntryTO getAuditTO(final String key, final AuditEntry auditEntry) {
        AuditEntryTO auditEntryTO = new AuditEntryTO();
        auditEntryTO.setKey(key);
        auditEntryTO.setWho(auditEntry.getWho());
        auditEntryTO.setDate(auditEntry.getDate());
        auditEntryTO.setThrowable(auditEntry.getThrowable());
        auditEntryTO.setLoggerName(auditEntry.getLogger().toLoggerName());

        auditEntryTO.setSubCategory(auditEntry.getLogger().getSubcategory());
        auditEntryTO.setEvent(auditEntry.getLogger().getEvent());

        if (auditEntry.getLogger().getResult() != null) {
            auditEntryTO.setResult(auditEntry.getLogger().getResult().name());
        }

        if (auditEntry.getBefore() != null) {
            auditEntryTO.setBefore(POJOHelper.serializeWithDefaultPrettyPrinter(auditEntry.getBefore()));
        }

        if (auditEntry.getInput() != null) {
            auditEntryTO.getInputs().addAll(Arrays.stream(auditEntry.getInput()).
                    filter(Objects::nonNull).
                    map(POJOHelper::serializeWithDefaultPrettyPrinter).
                    collect(Collectors.toList()));
        }

        if (auditEntry.getOutput() != null) {
            auditEntryTO.setOutput(POJOHelper.serializeWithDefaultPrettyPrinter(auditEntry.getOutput()));
        }

        return auditEntryTO;
    }
}
