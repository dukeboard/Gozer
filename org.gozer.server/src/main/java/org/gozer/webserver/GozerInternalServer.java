package org.gozer.webserver;

/**
 * Created with IntelliJ IDEA.
 * User: sebastien
 * Date: 03/04/12
 * Time: 23:08
 * To change this template use File | Settings | File Templates.
 */
public class GozerInternalServer extends Acme.Serve.Serve {

        public void setMappingTable(PathTreeDictionary mappingtable) {
            super.setMappingTable(mappingtable);
        }

}
