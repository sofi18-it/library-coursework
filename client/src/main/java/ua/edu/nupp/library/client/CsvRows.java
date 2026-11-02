package ua.edu.nupp.library.client;

import java.util.ArrayList;
import java.util.List;

final class CsvRows {
    private CsvRows() { }
    static List<String> parse(String line) {
        List<String> fields=new ArrayList<>();StringBuilder field=new StringBuilder();boolean quoted=false;
        for(int i=0;i<line.length();i++) {
            char c=line.charAt(i);
            if(c=='"') { if(quoted&&i+1<line.length()&&line.charAt(i+1)=='"'){field.append('"');i++;}else quoted=!quoted; }
            else if(c==','&&!quoted){fields.add(field.toString().trim());field.setLength(0);}
            else field.append(c);
        }
        if(quoted)throw new IllegalArgumentException("Незакрита подвійна лапка.");
        fields.add(field.toString().trim());return fields;
    }
}
