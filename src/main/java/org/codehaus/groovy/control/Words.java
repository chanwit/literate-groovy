package org.codehaus.groovy.control;

import java.util.HashMap;
import java.util.Map;

public class Words {

    private static Map<String, String> THAI = new HashMap<String, String>() {{
        put("กำหนดให้", "given:");
        put("ทำงานเสร็จ", "cleanup:");
        put("ทำเสร็จ", "cleanup:");
        put("และ", "and:");
        put("แล้ว", "then:");
        put("เมื่อ", "when:");
        put("ตั้งค่า", "setup:");
        put("เตรียมการ", "setup:");
        put("โดยที่", "where:");
    }};

    private static Map<String, String> ENGLISH = new HashMap<String, String>() {{
        put("cleaning up", "cleanup:");
        put("clean up", "cleanup:");
        put("and", "and:");
        put("when", "when:");
        put("then", "then:");
        put("setting up", "setup:");
        put("set up", "setup:");
        put("given", "given:");
        put("where", "where:");
    }};

    static String getBlockVerb(Map<String, String> map, String s) {
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (s.startsWith(e.getKey())) {
                return e.getValue();
            }
        }

        return null;
    }

    static String getBlockVerb(String s) {
        String result = getBlockVerb(THAI, s);
        if (result != null) {
            return result;
        }

        return getBlockVerb(ENGLISH, s);
    }

}
