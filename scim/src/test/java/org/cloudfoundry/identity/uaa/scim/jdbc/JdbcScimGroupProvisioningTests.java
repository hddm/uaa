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
package org.cloudfoundry.identity.uaa.scim.jdbc;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.cloudfoundry.identity.uaa.rest.jdbc.JdbcPagingListFactory;
import org.cloudfoundry.identity.uaa.scim.ScimGroup;
import org.cloudfoundry.identity.uaa.scim.ScimGroupMember;
import org.cloudfoundry.identity.uaa.scim.exception.ScimResourceNotFoundException;
import org.cloudfoundry.identity.uaa.scim.test.TestUtils;
import org.cloudfoundry.identity.uaa.test.JdbcTestBase;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.StringUtils;

public class JdbcScimGroupProvisioningTests extends JdbcTestBase {

    private JdbcScimGroupProvisioning dao;

    private static final String addGroupSqlFormat = "insert into groups (id, displayName) values ('%s','%s')";

    private static final String SQL_INJECTION_FIELDS = "displayName,version,created,lastModified";

    private int existingGroupCount = -1;

    @Before
    public void initJdbcScimGroupProvisioningTests() {
        dao = new JdbcScimGroupProvisioning(jdbcTemplate, new JdbcPagingListFactory(jdbcTemplate, limitSqlAdapter));

        addGroup("g1", "uaa.user");
        addGroup("g2", "uaa.admin");
        addGroup("g3", "openid");

        validateGroupCount(3);
    }
    private void validateGroupCount(int expected) {
        existingGroupCount = jdbcTemplate.queryForInt("select count(id) from groups");
        assertEquals(expected, existingGroupCount);
    }

    private void validateGroup(ScimGroup group, String name) {
        assertNotNull(group);
        assertNotNull(group.getId());
        assertNotNull(group.getDisplayName());
        if (StringUtils.hasText(name)) {
            assertEquals(name, group.getDisplayName());
        }
    }

    @Test
    public void canRetrieveGroups() throws Exception {
        List<ScimGroup> groups = dao.retrieveAll();
        assertEquals(3, groups.size());
        for (ScimGroup g : groups) {
            validateGroup(g, null);
        }
    }

    @Test
    public void canRetrieveGroupsWithFilter() throws Exception {
        assertEquals(1, dao.query("displayName eq \"uaa.user\"").size());
        assertEquals(3, dao.query("displayName pr").size());
        assertEquals(1, dao.query("displayName eq \"openid\"").size());
        assertEquals(1, dao.query("DISPLAYNAMe eq \"uaa.admin\"").size());
        assertEquals(1, dao.query("displayName EQ \"openid\"").size());
        assertEquals(1, dao.query("displayName eq \"Openid\"").size());
        assertEquals(1, dao.query("displayName co \"user\"").size());
        assertEquals(3, dao.query("id sw \"g\"").size());
        assertEquals(3, dao.query("displayName gt \"oauth\"").size());
        assertEquals(0, dao.query("displayName lt \"oauth\"").size());
        assertEquals(1, dao.query("displayName eq \"openid\" and meta.version eq 0").size());
        assertEquals(3, dao.query("meta.created gt \"1970-01-01T00:00:00.000Z\"").size());
        assertEquals(3, dao.query("displayName pr and id co \"g\"").size());
        assertEquals(2, dao.query("displayName eq \"openid\" or displayName co \".user\"").size());
        assertEquals(3, dao.query("displayName eq \"foo\" or id sw \"g\"").size());
    }

    @Test
    public void canRetrieveGroupsWithFilterAndSortBy() {
        assertEquals(3, dao.query("displayName pr", "id", true).size());
        assertEquals(1, dao.query("id co \"2\"", "displayName", false).size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotRetrieveGroupsWithIllegalQuotesFilter() {
        assertEquals(1, dao.query("displayName eq \"bar").size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotRetrieveGroupsWithMissingQuotesFilter() {
        assertEquals(0, dao.query("displayName eq bar").size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotRetrieveGroupsWithInvalidFieldsFilter() {
        assertEquals(1, dao.query("name eq \"openid\"").size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotRetrieveGroupsWithWrongFilter() {
        assertEquals(0, dao.query("displayName pr \"r\"").size());
    }

    @Test
    public void canRetrieveGroup() throws Exception {
        ScimGroup group = dao.retrieve("g1");
        validateGroup(group, "uaa.user");
    }

    @Test(expected = ScimResourceNotFoundException.class)
    public void cannotRetrieveNonExistentGroup() {
        dao.retrieve("invalidgroup");
    }

    @Test
    public void canCreateGroup() throws Exception {
        ScimGroup g = new ScimGroup("", "test.1");
        ScimGroupMember m1 = new ScimGroupMember("m1", ScimGroupMember.Type.USER, ScimGroupMember.GROUP_MEMBER);
        ScimGroupMember m2 = new ScimGroupMember("m2", ScimGroupMember.Type.USER, ScimGroupMember.GROUP_ADMIN);
        g.setMembers(Arrays.asList(m1, m2));
        g = dao.create(g);
        validateGroupCount(4);
        validateGroup(g, "test.1");
    }

    @Test
    public void canDeleteGroupsUsingFilter1() throws Exception {
        dao.delete("displayName eq \"uaa.user\"");
        validateGroupCount(2);
    }

    @Test
    public void canDeleteGroupsUsingFilter2() throws Exception {
        dao.delete("displayName sw \"uaa\"");
        validateGroupCount(1);
    }

    @Test
    public void canDeleteGroupsUsingFilter3() throws Exception {
        dao.delete("id eq \"g1\"");
        validateGroupCount(2);
    }

    @Test
    public void canUpdateGroup() throws Exception {
        ScimGroup g = dao.retrieve("g1");
        assertEquals("uaa.user", g.getDisplayName());

        ScimGroupMember m1 = new ScimGroupMember("m1", ScimGroupMember.Type.USER, ScimGroupMember.GROUP_MEMBER);
        ScimGroupMember m2 = new ScimGroupMember("g2", ScimGroupMember.Type.USER, ScimGroupMember.GROUP_ADMIN);
        g.setMembers(Arrays.asList(m1, m2));
        g.setDisplayName("uaa.none");

        g = dao.update("g1", g);

        g = dao.retrieve("g1");
        validateGroup(g, "uaa.none");
    }

    @Test
    public void canRemoveGroup() throws Exception {
        dao.delete("g1", 0);
        validateGroupCount(2);
    }

    private void addGroup(String id, String name) {
        TestUtils.assertNoSuchUser(jdbcTemplate, "id", id);
        jdbcTemplate.execute(String.format(addGroupSqlFormat, id, name));
    }

    @Test(expected = IllegalArgumentException.class)
    public void sqlInjectionAttack1Fails() {
        dao.query("displayName='something'; select " + SQL_INJECTION_FIELDS
                        + " from groups where displayName='something'");
    }

    @Test(expected = IllegalArgumentException.class)
    public void sqlInjectionAttack2Fails() {
        dao.query("displayName gt 'a'; select " + SQL_INJECTION_FIELDS
                        + " from groups where displayName='something'");
    }

    @Test(expected = IllegalArgumentException.class)
    public void sqlInjectionAttack3Fails() {
        dao.query("displayName eq \"something\"; select " + SQL_INJECTION_FIELDS
                        + " from groups where displayName='something'");
    }

    @Test(expected = IllegalArgumentException.class)
    public void sqlInjectionAttack4Fails() {
        dao.query("displayName eq \"something\"; select id from groups where id='''; select " + SQL_INJECTION_FIELDS
                        + " from groups where displayName='something'");
    }

    @Test(expected = IllegalArgumentException.class)
    public void sqlInjectionAttack5Fails() {
        dao.query("displayName eq \"something\"'; select " + SQL_INJECTION_FIELDS
                        + " from groups where displayName='something''");
    }
}
