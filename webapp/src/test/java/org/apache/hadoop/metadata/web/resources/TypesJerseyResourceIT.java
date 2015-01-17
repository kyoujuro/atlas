/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.metadata.web.resources;

import com.google.common.collect.ImmutableList;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.hadoop.metadata.json.TypesSerialization;
import org.apache.hadoop.metadata.types.AttributeDefinition;
import org.apache.hadoop.metadata.types.ClassType;
import org.apache.hadoop.metadata.types.DataTypes;
import org.apache.hadoop.metadata.types.HierarchicalTypeDefinition;
import org.apache.hadoop.metadata.types.Multiplicity;
import org.apache.hadoop.metadata.types.StructTypeDefinition;
import org.apache.hadoop.metadata.types.TraitType;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Integration test for types jersey resource.
 */
public class TypesJerseyResourceIT extends BaseResourceIT {

    private List<HierarchicalTypeDefinition> typeDefinitions;

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();

        typeDefinitions = createHiveTypes();
    }

    @AfterClass
    public void tearDown() throws Exception {
        typeDefinitions.clear();
    }

    @Test
    public void testSubmit() throws Exception {
        for (HierarchicalTypeDefinition typeDefinition : typeDefinitions) {
            String typesAsJSON = TypesSerialization.toJson(
                    typeSystem, typeDefinition.typeName);
            System.out.println("typesAsJSON = " + typesAsJSON);

            WebResource resource = service
                    .path("api/metadata/types/submit")
                    .path(typeDefinition.typeName);

            ClientResponse clientResponse = resource
                    .accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON)
                    .method(HttpMethod.POST, ClientResponse.class, typesAsJSON);
            Assert.assertEquals(clientResponse.getStatus(), Response.Status.OK.getStatusCode());

            String responseAsString = clientResponse.getEntity(String.class);
            Assert.assertNotNull(responseAsString);

            JSONObject response = new JSONObject(responseAsString);
            Assert.assertEquals(response.get("typeName"), typeDefinition.typeName);
            Assert.assertNotNull(response.get("types"));
            Assert.assertNotNull(response.get("requestId"));
        }
    }

    @Test (dependsOnMethods = "testSubmit")
    public void testGetDefinition() throws Exception {
        for (HierarchicalTypeDefinition typeDefinition : typeDefinitions) {
            System.out.println("typeName = " + typeDefinition.typeName);

            WebResource resource = service
                    .path("api/metadata/types/definition")
                    .path(typeDefinition.typeName);

            ClientResponse clientResponse = resource
                    .accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON)
                    .method(HttpMethod.GET, ClientResponse.class);
            Assert.assertEquals(clientResponse.getStatus(), Response.Status.OK.getStatusCode());

            String responseAsString = clientResponse.getEntity(String.class);
            Assert.assertNotNull(responseAsString);

            JSONObject response = new JSONObject(responseAsString);
            Assert.assertEquals(response.get("typeName"), typeDefinition.typeName);
            Assert.assertNotNull(response.get("definition"));
            Assert.assertNotNull(response.get("requestId"));
        }
    }

    @Test
    public void testGetDefinitionForNonexistentType() throws Exception {
        WebResource resource = service
                .path("api/metadata/types/definition")
                .path("blah");

        ClientResponse clientResponse = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .method(HttpMethod.GET, ClientResponse.class);
        Assert.assertEquals(clientResponse.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test (dependsOnMethods = "testSubmit")
    public void testGetTypeNames() throws Exception {
        WebResource resource = service
                .path("api/metadata/types/list");

        ClientResponse clientResponse = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .method(HttpMethod.GET, ClientResponse.class);
        Assert.assertEquals(clientResponse.getStatus(), Response.Status.OK.getStatusCode());

        String responseAsString = clientResponse.getEntity(String.class);
        Assert.assertNotNull(responseAsString);

        JSONObject response = new JSONObject(responseAsString);
        Assert.assertNotNull(response.get("requestId"));

        final JSONArray list = response.getJSONArray("list");
        Assert.assertNotNull(list);
    }

    private List<HierarchicalTypeDefinition> createHiveTypes() throws Exception {
        ArrayList<HierarchicalTypeDefinition> typeDefinitions = new ArrayList<>();

        HierarchicalTypeDefinition<ClassType> databaseTypeDefinition =
                createClassTypeDef("database",
                        ImmutableList.<String>of(),
                        createRequiredAttrDef("name", DataTypes.STRING_TYPE),
                        createRequiredAttrDef("description", DataTypes.STRING_TYPE));
        typeDefinitions.add(databaseTypeDefinition);

        HierarchicalTypeDefinition<ClassType> tableTypeDefinition = createClassTypeDef(
                "table",
                ImmutableList.<String>of(),
                createRequiredAttrDef("name", DataTypes.STRING_TYPE),
                createRequiredAttrDef("description", DataTypes.STRING_TYPE),
                createRequiredAttrDef("type", DataTypes.STRING_TYPE),
                new AttributeDefinition("database",
                        "database", Multiplicity.REQUIRED, false, "database"));
        typeDefinitions.add(tableTypeDefinition);

        HierarchicalTypeDefinition<TraitType> fetlTypeDefinition = createTraitTypeDef(
                "fetl",
                ImmutableList.<String>of(),
                createRequiredAttrDef("level", DataTypes.INT_TYPE));
        typeDefinitions.add(fetlTypeDefinition);

        typeSystem.defineTypes(
                ImmutableList.<StructTypeDefinition>of(),
                ImmutableList.of(fetlTypeDefinition),
                ImmutableList.of(databaseTypeDefinition, tableTypeDefinition));

        return typeDefinitions;
    }
}
