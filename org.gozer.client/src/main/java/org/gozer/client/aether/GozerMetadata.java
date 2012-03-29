package org.gozer.client.aether;

import org.sonatype.aether.metadata.Metadata;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 29/03/12
 * Time: 02:51
 */
public class GozerMetadata implements Metadata {
    
    private String groupID = "";
    private String artifactID = "";
    private String version = "";
    private String type = "";
    private File file = null;
    private Nature nature = null;
    
    public GozerMetadata(Metadata m){
        groupID = m.getGroupId();
        artifactID = m.getArtifactId();
        version = m.getVersion();
        type = m.getType();
        file = m.getFile();
        nature = m.getNature();
    }
    
    @Override
    public String getGroupId() {
        return groupID;
    }

    @Override
    public String getArtifactId() {
        return artifactID;
    }

    @Override
    public String getVersion() {
        return version;  
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Nature getNature() {
        return nature;
    }

    @Override
    public File getFile() {
        return file;
    }
    
    @Override
    public Metadata setFile(File file) {
        String resource = file.getAbsolutePath().replace("maven-metadata", "gozer-metadata"); //Hack
        resource = resource.substring(0, resource.length() - 4) + ".zip"; //Hack
        GozerMetadata newMeta = new  GozerMetadata(this);
        this.file = new File(resource);
        return this;
    }


}
