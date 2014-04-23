package org.networklibrary.edger.parsing;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.networklibrary.core.parsing.Parser;
import org.networklibrary.core.parsing.ParsingErrorException;
import org.networklibrary.core.types.EdgeData;

public class TabFileParser implements Parser<EdgeData>{

	private List<String> columns = null;
	
	public TabFileParser(String header) {
		columns = Arrays.asList(header.split("\\t",-1));
	}

	public Collection<EdgeData> parse(String line) {
		
		return null;
	}

	public boolean hasHeader() {
		// TODO Auto-generated method stub
		return false;
	}

	public void parseHeader(String header) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean hasExtraParameters() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void takeExtraParameters(List<String> extras) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDataSource(String location) throws ParsingErrorException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean ready() throws ParsingErrorException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<EdgeData> parse() throws ParsingErrorException {
		// TODO Auto-generated method stub
		return null;
	}

//	public Collection<EdgeData> parse(String line) {
//		List<EdgeData> res = null;
//		
//		if(!line.isEmpty()){
//			res = new LinkedList<EdgeData>();
//			
//			String[] values =line.split("\\t",-1);
//			
//			if(values.length != columns.size()){
//				throw new IllegalArgumentException("number of elements in row does not match number of columns " + line);
//			}
//			
//			for(int i = 0; i < values.length; ++i){
//				if(!values[i].isEmpty()){
//					
//				}
//			}
//			
//		}
//		
//		return res;
//	}

	
}
