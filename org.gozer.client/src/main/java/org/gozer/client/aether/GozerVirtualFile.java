package org.gozer.client.aether;

import java.io.File;
import java.net.URI;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 29/03/12
 * Time: 01:36
 */
public class GozerVirtualFile extends File {
    public GozerVirtualFile() {
        super("gozer:vfs");
    }
}
