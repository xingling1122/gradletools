package com.yy.tools

import groovy.xml.QName
import org.codehaus.groovy.runtime.InvokerHelper

/**
 * 去掉转义
 */
class CustomXmlNodePrinter extends XmlNodePrinter {

    CustomXmlNodePrinter(PrintWriter out) {
        super(out)
    }

    @Override
    protected void printNameAttributes(Map attributes, NamespaceContext ctx) {
        if (attributes == null || attributes.isEmpty()) {
            return
        }
        for (Object p : attributes.entrySet()) {
            Map.Entry entry = (Map.Entry) p
            out.print(" ")
            out.print(getName(entry.getKey()))
            out.print("=")
            Object value = entry.getValue()
            out.print(quote)
            if (value instanceof String) {
                out.print((String) value)
            } else {
                out.print(InvokerHelper.toString(value))
            }
            out.print(quote)
            printNamespace(entry.getKey(), ctx)
        }
    }

    private String getName(Object object) {
        if (object instanceof String) {
            return (String) object
        } else if (object instanceof QName) {
            QName qname = (QName) object
            if (!namespaceAware) {
                return qname.getLocalPart()
            }
            return qname.getQualifiedName()
        } else if (object instanceof Node) {
            Object name = ((Node) object).name()
            return getName(name)
        }
        return object.toString()
    }

    @Override
    protected void printSimpleItem(Object value) {
        if (!preserveWhitespace) printLineBegin()
        out.print(InvokerHelper.toString(value))
        if (!preserveWhitespace) printLineEnd()
    }

//    private void printEscaped(String s) {
//        for (int i = 0; i < s.length(); i++) {
//            char c = s.charAt(i)
//            switch (c) {
//                case '<':
//                    out.print("&lt;")
//                    break
//                case '>':
//                    out.print("&gt;")
//                    break
//                case '&':
//                    out.print("&amp;")
//                    break
//                case '\'':
//                    out.print("&apos;")
//                    break
//                case '"':
//                    out.print("&quot;")
//                    break
//                default:
//                    out.print(c)
//            }
//        }
//    }
}