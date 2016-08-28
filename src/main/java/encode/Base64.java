package encode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Base64 {
	public static Map <Character,Integer> map=new HashMap<Character,Integer>();
	static {
		for (int i = 0; i < 26; i++) {
			map.put((char) (i + 'A'), i);
		}
		for (int i = 0; i < 26; i++) {
			map.put((char) (i + 'a'), i+26);
		}
		for (int i = 0; i < 10; i++) {
			map.put((char) (i + '0'), i+52);
		}
		map.put('+', 62);
		map.put('/', 63);
	}

	public List<Character> encode(char[] code) {
		ArrayList<Character> liost=new ArrayList<Character>();
		for(int i=0;i<code.length;i+=4){
			byte one = make(code[i]);
			byte two = make(code[i+1]);
			byte three = make(code[i+2]);
			byte four = make(code[i+3]);
			
			byte fone=(byte) ((one<<2)|((two&0x30)>>4));
			byte ftwo=(byte) ((two<<4)|((three&0x3C)>>2));
			byte fthree=(byte) ((three<<6)|four);
			
			liost.add((char)(fone));
			liost.add((char)(ftwo));
			liost.add((char)(fthree));
		}
		return liost;
	}
	public byte make(char num){
		if(num=='=') return 0x00;
		return map.get(num).byteValue();
	}
	public int getpnum(char[] code, char temp) {
		int flag = 0;
		for (int i = code.length - 1; i > 0; i--) {
			if (code[i] == temp) {
				flag++;
			} else {
				break;
			}
		}
		return flag;
	}

	public static void main(String[] args) {
		String aa = "VGhlIHRlcm0gQmFzZTY0IHJlZmVycyB0byBhIHNwZWNpZmljIE1JTUUgY29udGVudCB0cmFuc2Zl"
				+ "ciBlbmNvZGluZy4gSXQgaXMgYWxzbyB1c2VkIGFzIGEgZ2VuZXJpYyB0ZXJtIGZvciBhbnkgc2lt"
				+ "aWxhciBlbmNvZGluZyBzY2hlbWUgdGhhdCBlbmNvZGVzIGJpbmFyeSBkYXRhIGJ5IHRyZWF0aW5n"
				+ "IGl0IG51bWVyaWNhbGx5IGFuZCB0cmFuc2xhdGluZyBpdCBpbnRvIGEgYmFzZSA2NCByZXByZXNl"
				+ "bnRhdGlvbi4gVGhlIHBhcnRpY3VsYXIgY2hvaWNlIG9mIGJhc2UgaXMgZHVlIHRvIHRoZSBoaXN0"
				+ "b3J5IG9mIGNoYXJhY3RlciBzZXQgZW5jb2Rpbmc6IG9uZSBjYW4gY2hvb3NlIGEgc2V0IG9mIDY0"
				+ "IGNoYXJhY3RlcnMgdGhhdCBpcyBib3RoIHBhcnQgb2YgdGhlIHN1YnNldCBjb21tb24gdG8gbW9z"
				+ "dCBlbmNvZGluZ3MsIGFuZCBhbHNvIHByaW50YWJsZS4gVGhpcyBjb21iaW5hdGlvbiBsZWF2ZXMg"
				+ "dGhlIGRhdGEgdW5saWtlbHkgdG8gYmUgbW9kaWZpZWQgaW4gdHJhbnNpdCB0aHJvdWdoIHN5c3Rl"
				+ "bXMsIHN1Y2ggYXMgZW1haWwsIHdoaWNoIHdlcmUgdHJhZGl0aW9uYWxseSBub3QgOC1iaXQgY2xl"
				+ "YW4uDQpUaGUgcHJlY2lzZSBjaG9pY2Ugb2YgY2hhcmFjdGVycyBpcyBkaWZmaWN1bHQuIFRoZSBl"
				+ "YXJsaWVzdCBpbnN0YW5jZXMgb2YgdGhpcyB0eXBlIG9mIGVuY29kaW5nIHdlcmUgY3JlYXRlZCBm"
				+ "b3IgZGlhbHVwIGNvbW11bmljYXRpb24gYmV0d2VlbiBzeXN0ZW1zIHJ1bm5pbmcgdGhlIHNhbWUg"
				+ "T1MgLSBlLmcuIFV1ZW5jb2RlIGZvciBVTklYLCBCaW5IZXggZm9yIHRoZSBUUlMtODAgKGxhdGVy"
				+ "IGFkYXB0ZWQgZm9yIHRoZSBNYWNpbnRvc2gpIC0gYW5kIGNvdWxkIHRoZXJlZm9yZSBtYWtlIG1v"
				+ "cmUgYXNzdW1wdGlvbnMgYWJvdXQgd2hhdCBjaGFyYWN0ZXJzIHdlcmUgc2FmZSB0byB1c2UuIEZv"
				+ "ciBpbnN0YW5jZSwgVXVlbmNvZGUgdXNlcyB1cHBlcmNhc2UgbGV0dGVycywgZGlnaXRzLCBhbmQg"
				+ "bWFueSBwdW5jdHVhdGlvbiBjaGFyYWN0ZXJzLCBidXQgbm8gbG93ZXJjYXNlLCBzaW5jZSBVTklY"
				+ "IHdhcyBzb21ldGltZXMgdXNlZCB3aXRoIHRlcm1pbmFscyB0aGF0IGRpZCBub3Qgc3VwcG9ydCBk"
				+ "aXN0aW5jdCBsZXR0ZXIgY2FzZS4gVW5mb3J0dW5hdGVseSBmb3IgaW50ZXJvcGVyYWJpbGl0eSB3"
				+ "aXRoIG5vbi1VTklYIHN5c3RlbXMsIHNvbWUgb2YgdGhlIHB1bmN0dWF0aW9uIGNoYXJhY3RlcnMg"
				+ "ZG8gbm90IGV4aXN0IGluIG90aGVyIHRyYWRpdGlvbmFsIGNoYXJhY3RlciBzZXRzLiBUaGUgTUlN"
				+ "RSBCYXNlNjQgZW5jb2RpbmcgcmVwbGFjZXMgbW9zdCBvZiB0aGUgcHVuY3R1YXRpb24gY2hhcmFj"
				+ "dGVycyB3aXRoIHRoZSBsb3dlcmNhc2UgbGV0dGVycywgYSByZWFzb25hYmxlIHJlcXVpcmVtZW50"
				+ "IGJ5IHRoZSB0aW1lIGl0IHdhcyBkZXNpZ25lZC4=";
		String s="TWFu";
		Base64 s1=new Base64();
		List<Character> list=s1.encode(aa.toCharArray());
		for(char sss:list){
			System.out.print(sss);
		}
		
	}
}
