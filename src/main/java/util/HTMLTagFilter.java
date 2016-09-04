package util;

import java.io.*;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 去除html 书签的重复页面
 */
public class HTMLTagFilter {

   public static void main(String[] args) throws IOException {

      Pattern r = Pattern.compile(".*HREF=\"(.+?)\" .*");

      Set<String> set=new HashSet<String>();
      List<String> lines = new ArrayList<String>();

      File s =new File("D://like/");

      File[] as = s.listFiles();

      for(File f:as){
         testOneFile(f,set,lines,r);
      }

      BufferedWriter bw  =new BufferedWriter(new OutputStreamWriter(new FileOutputStream("D://aaa.txt")));

      for(String t :lines){
         bw.write(t);
         bw.newLine();
      }
      bw.close();
   }

   private static void testOneFile(File fileName, Set<String> set, List<String> lines, Pattern r) throws IOException {

      FileInputStream fis =new FileInputStream(fileName);
      BufferedReader fr =new BufferedReader(new InputStreamReader(fis,"UTF-8"));
      String line ;
      while((line=fr.readLine())!=null){
//       System.out.println(line);
         Matcher m =r.matcher(line);
         if(m.find()){
            String href = m.group(1);
            if(!set.contains(href)){
               set.add(href);
               lines.add(line);
            }
         }
      }
      fr.close();
      fis.close();

   }
}
