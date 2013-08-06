package org.commonjava.aprox.depgraph.json;

import java.lang.reflect.Type;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class ProjectVersionRefSer
    implements JsonSerializer<ProjectVersionRef>, JsonDeserializer<ProjectVersionRef>
{

    @Override
    public ProjectVersionRef deserialize( final JsonElement src, final Type type, final JsonDeserializationContext ctx )
        throws JsonParseException
    {
        return JsonUtils.parseProjectVersionRef( src.getAsString() );
    }

    @Override
    public JsonElement serialize( final ProjectVersionRef src, final Type type, final JsonSerializationContext ctx )
    {
        return new JsonPrimitive( JsonUtils.formatRef( src ) );
    }

}