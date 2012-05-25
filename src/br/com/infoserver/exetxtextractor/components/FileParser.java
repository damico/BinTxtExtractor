package br.com.infoserver.exetxtextractor.components;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import br.com.infoserver.exetxtextractor.commons.LoggerManager;
import br.com.infoserver.exetxtextractor.commons.Utils;

public class FileParser {
	private ArrayList<String> passChange = new ArrayList<String>();
	
	char[][] senhas; 
	int pwdMatchCounter;
	boolean writeFile = false;
	
	private String strings = null;
	private StringBuffer sbStrings = new StringBuffer();

	public String getStrings() {
		return strings;
	}

	public void setStrings(String strings) {
		this.strings = strings;
	}

	public FileParser(String pwdPatterns, boolean writeFile) {
		this.senhas = Utils.getInstance().getBiDiCharArrayFromText(pwdPatterns);
		LoggerManager.getInstance().logAtDebugTime(this.getClass().getName(), "pwdPatterns: "+pwdPatterns+" loaded with "+this.senhas.length+" patterns.");
		this.writeFile = writeFile;
	}

	public int nogueiraSpell(String source, boolean ucs2) throws Exception {
		int resultCounter = 0;
		File input = new File(source);
		File nwDr = new File(input.getParent()+"/encharmed");
		
		nwDr.mkdir();
		input.getAbsoluteFile();
		String target = nwDr.getAbsolutePath()+"/"+input.getAbsoluteFile().getName();
		
		FileInputStream inSt = new FileInputStream(input);
		FileOutputStream outSt = new FileOutputStream(new File(target));
		char arqChar;
		int intChar;
		List<Character> buffer = new ArrayList<Character>();
		char[] inicio = "PWD=".toCharArray();
		char[] inicioMin = "pwd=".toCharArray();
		int matchCounter = 0;
		boolean match = false;
		while ((intChar = inSt.read()) != -1) {
			arqChar = (char) intChar;
			if (!match) {
				if (arqChar >= 33 && arqChar <= 126) {
					if (arqChar == inicio[matchCounter] || arqChar == inicioMin[matchCounter]) {
						matchCounter++;
						if (matchCounter == inicio.length) {
							match = true;
							matchCounter = 0;
						}
					} else matchCounter = 0;
				}
				if(writeFile) outSt.write(arqChar);
			} else {
				if(arqChar == ';' || (buffer.size() > 0 && ((int)buffer.get(buffer.size() - 1) == 0 || !ucs2) && intChar == 0)){
					for(char chrBuf : buffer){
						if((chrBuf >= 65 && chrBuf <= 90)) chrBuf += 32;
						outSt.write(chrBuf);
					}
					if(writeFile) outSt.write(arqChar);
					buffer = new ArrayList<Character>();
					match = false;
					resultCounter++;
				}else buffer.add(arqChar);
			}
		}
		inSt.close();
		outSt.close();
		if(!writeFile){
			File outFile = new File(target);
			outFile.delete();
		}
		trackMd5(source, target);
		return resultCounter;
	}
	
	public int nogueiraSpellByPass(String source, boolean ucs2) throws Exception {
		setPassChange(new ArrayList<String>());
		int resultCounter = 0;
		
		File input = new File(source);
		File nwDr = new File(input.getParent()+"/encharmed");
		
		nwDr.mkdir();
		input.getAbsoluteFile();
		
		String target = nwDr.getAbsolutePath()+"/"+input.getAbsoluteFile().getName();
		
		FileInputStream inSt = new FileInputStream(input);
		FileOutputStream outSt = new FileOutputStream(new File(target));
		char arqChar;
		int intChar, charAnt = 0;
		char[] buffer = new char[50];
		int bufferSize = 0;
		
		char[] inicio = "PWD=".toCharArray();
		char[] inicioMin = "pwd=".toCharArray();

		int matchCounter = 0;
		pwdMatchCounter = 0;
		boolean match = false;
		boolean[] matches = new boolean[senhas.length];
		
		while ((intChar = inSt.read()) != -1) {
			arqChar = (char) intChar;
			if(match) {
				if(arqChar == ';' || (bufferSize > 0 && ((int)buffer[bufferSize - 1] == 0 || !ucs2) && intChar == 0)){
					boolean found = false;
					for(int i = 0; i < matches.length; i++){
						if(matches[i] && senhas[i].length == pwdMatchCounter){
							found = true;
						}
					}
					if(found){
						StringBuffer pasbuf = new StringBuffer();
						for(int i = 0; i < bufferSize; i++){
							if(buffer[i] != 0){
								pasbuf.append(buffer[i]);
							}
							if((buffer[i] >= 65 && buffer[i] <= 90)) buffer[i] += 32;
							if(writeFile) outSt.write(buffer[i]);
						}
						bufferSize = 0;
						match = false;
						resultCounter++;
						passChange.add(pasbuf.toString());
					}
					else { 
						for(int i = 0; i < bufferSize; i++) if(writeFile) outSt.write(buffer[i]);
						match = false;
						bufferSize = 0;
					}
				}
				else if((ucs2 && intChar == 0 && (bufferSize == 0 || (int)buffer[bufferSize - 1] != 0)) || verificaPalavra(matches, arqChar)){
					buffer[bufferSize++] = arqChar;
				}
				else {
					for(int i = 0; i < bufferSize; i++) if(writeFile) outSt.write(buffer[i]); 
					match = false;
					bufferSize = 0;;
				}
			}
			if (!match) {
				if (arqChar >= 33 && arqChar <= 126) {
					if(arqChar >= 33 && arqChar <= 43) sbStrings.append("\n");
					else sbStrings.append(arqChar);
					if (arqChar == inicio[matchCounter] || arqChar == inicioMin[matchCounter]) {
						matchCounter++;
						if (matchCounter == inicio.length) {
							match = true;
							matchCounter = 0;
							resetMatch(matches);
							pwdMatchCounter = 0;
						}
					} else matchCounter = 0;
				}
				else if ((intChar == 0 && (!ucs2 || charAnt == 0)) || arqChar == '='){
					match = true;
					resetMatch(matches);
					pwdMatchCounter = 0;
					matchCounter = 0;
				}
				if(writeFile) outSt.write(arqChar);
			}
			charAnt = intChar;

		}
		for(int i = 0; i < bufferSize; i++) if(writeFile) outSt.write(buffer[i]);
		inSt.close();
		outSt.close();
		if(!writeFile){
			File outFile = new File(target);
			outFile.delete();
		}
		
		trackMd5(source, target);
		
		setStrings(sbStrings.toString());
		
		return resultCounter;
	}

	private void trackMd5(String source, String target) throws NoSuchAlgorithmException, FileNotFoundException {
		String sourceMd5 = Utils.getInstance().getMd5FromFile(source);
		String targetMd5 = Utils.getInstance().getMd5FromFile(target);
		LoggerManager.getInstance().logAtDebugTime(this.getClass().getName(), "MD5 Track: source "+source+" ["+sourceMd5+"]");
		LoggerManager.getInstance().logAtDebugTime(this.getClass().getName(), "MD5 Track: target "+target+" ["+targetMd5+"]");
	}
	
	private boolean verificaPalavra(boolean[] matches, char inputChar){
		boolean match = false;
		for(int i = 0; i < senhas.length; i++){
			if(matches[i]){
				if(pwdMatchCounter < senhas[i].length && senhas[i][pwdMatchCounter] == inputChar){
					match = true;
				}
				else{
					matches[i] = false;
				}
			}
		}
		if(match)
			pwdMatchCounter++;
		return match;
	}
	
	private void resetMatch(boolean[] matches){
		for(int i = 0; i < matches.length; i++){
			matches[i] = true;
		}
	}

	public ArrayList<String> getPassChange() {
		return passChange;
	}

	public void setPassChange(ArrayList<String> passChange) {
		this.passChange = passChange;
	}
}
