/*
 * 
 * 
 */
package org.gozer.diff;

import javolution.util.FastList;

import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Used to keep track of difference between 2 zip files.
 * 
 * @author Sean C. Sullivan
 */
public class Differences {
    private final List<String> added = new FastList<String>();
    private final List<String> removed = new FastList<String>();
    private final List<String> changed = new FastList<String>();
    private ZipFile filename1;
    private ZipFile filename2;
    public Differences() {
        // todo 
    }

    public void setFilename1(ZipFile filename) {
        filename1 = filename;
    }

    public void setFilename2(ZipFile filename) {
        filename2 = filename;
    }

    public ZipFile getFilename1() {
        return filename1;
    }

    public ZipFile getFilename2() {
        return filename2;
    }

    public void fileAdded(String fqn) {
        added.add(fqn);
    }

    public void fileRemoved(String fqn) {
        removed.add(fqn);
    }

    public void fileChanged(String fqn) {
        changed.add(fqn);
    }

    public List<String> getAdded() {
        return added;
    }

    public List<String> getRemoved() {
        return removed;
    }

    public List<String> getChanged() {
        return changed;
    }

    public boolean hasDifferences() {
        return ((getChanged().size() > 0) || (getAdded().size() > 0) || (getRemoved().size() > 0));
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        if (added.size() == 1) {
            sb.append("1 file was");
        } else {
            sb.append(added.size() + " files were");
        }
        sb.append(" added to " + this.getFilename2() + "\n");

        Iterator iter = added.iterator();
        while (iter.hasNext()) {
            String name = (String) iter.next();
            sb.append("\t[added] " + name + "\n");
        }

        if (removed.size() == 1) {
            sb.append("1 file was");
        } else {
            sb.append(removed.size() + " files were");
        }
        sb.append(" removed from " + this.getFilename2() + "\n");

        iter = removed.iterator();
        while (iter.hasNext()) {
            String name = (String) iter.next();
            sb.append("\t[removed] " + name + "\n");
        }

        if (changed.size() == 1) {
            sb.append("1 file changed\n");
        } else {
            sb.append(changed.size() + " files changed\n");
        }

        iter = getChanged().iterator();
        while (iter.hasNext()) {
            String name = (String) iter.next();
            //ZipEntry[] entries = (ZipEntry[]) getChanged().get(name);
            sb.append("\t[changed] " + name + " ");
            //sb.append(" ( size " + entries[0].getSize());
            //sb.append(" : " + entries[1].getSize());
            sb.append(" )\n");
        }
        int differenceCount = added.size() + changed.size() + removed.size();

        sb.append("Total differences: " + differenceCount);
        return sb.toString();
    }

}
