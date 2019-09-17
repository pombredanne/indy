/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.promote.ftest.rule;

import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.promote.model.GroupPromoteRequest;
import org.commonjava.indy.promote.model.GroupPromoteResult;
import org.commonjava.indy.promote.model.ValidationResult;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 9/23/15.
 */
public class NoSnapshots_PluginVersion_RuleTest
        extends AbstractValidationRuleTest<Group>
{

    private static final String RULE = "no-snapshots.groovy";

    @Test
    @Category( EventDependent.class )
    public void run()
            throws Exception
    {
        String invalid = "org/foo/invalid/1/invalid-1.pom";
        String valid = "org/foo/valid/1.1/valid-1.1.pom";

        deploy( invalid, "<?xml version=\"1.0\"?>\n<project><modelVersion>4.0.0</modelVersion><groupId>org.foo</groupId>"
                + "<artifactId>invalid</artifactId><version>1</version><build><plugins>"
                + "<plugin><groupId>org.bar</groupId><artifactId>plugin</artifactId>"
                + "<version>1.0-SNAPSHOT</version></plugin></plugins></build></project>" );
        deploy( valid, "<?xml version=\"1.0\"?>\n<project><modelVersion>4.0.0</modelVersion><groupId>org.foo</groupId>"
                        + "<artifactId>valid</artifactId><version>1.1</version><build><plugins>\"\n"
                + "                + \"<plugin><groupId>org.bar</groupId><artifactId>plugin</artifactId>\"\n"
                + "                + \"<version>1.0</version></plugin></plugins></build></project>" );

        waitForEventPropagation();

        GroupPromoteRequest request = new GroupPromoteRequest( source.getKey(), target.getName() );
        GroupPromoteResult result = module.promoteToGroup( request );
        assertThat( result, notNullValue() );

        ValidationResult validations = result.getValidations();
        assertThat( validations, notNullValue() );

        Map<String, String> validatorErrors = validations.getValidatorErrors();
        assertThat( validatorErrors, notNullValue() );

        System.out.println(validatorErrors);

        String errors = validatorErrors.get( RULE );
        assertThat( "There should have been some validation errors!", errors, notNullValue() );

        System.out.println(validatorErrors);
        assertThat( "Validation errors should not originate from: " + valid, errors.contains( valid ), equalTo( false ) );
        assertThat( "Validation errors should originate from: " + invalid, errors.contains( invalid ), equalTo( true ) );
    }

    public NoSnapshots_PluginVersion_RuleTest()
    {
        super( Group.class );
    }

    @Override
    protected String getRuleScriptFile()
    {
        return RULE;
    }

    @Override
    protected String getRuleScriptContent()
            throws IOException
    {
        String path = "promote/rules/" + RULE;
        return readTestResource( path );
    }

}
