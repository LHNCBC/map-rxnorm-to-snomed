package gov.nih.nlm.mor.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
//import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;


public class ExcelReport {
	private File file;
	private FileOutputStream out;
	private Workbook wb;
	private Sheet sheet;
	private Row r;
	private Cell c;
	private CellStyle cs;
	private CellStyle cs2;
	private Font f;
	private Font f2;
	private int rownum;
	private PrintWriter pw = null;
	private String outputFileText = null;
	private ArrayList<String> columnHeadings = null;
	private int sheetIndex = 0;
	private int columnNumbers = 0;
//	private NPOIFSFileSystem fs = null;
	
	public ExcelReport(String filename) {
//			NPOIFSFileSystem fs = new NPOIFSFileSystem(new File(filename));
			file = new File(filename);			
			try {
				out = new FileOutputStream(file);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			outputFileText = filename.replace(".xls", ".txt");
			wb = new HSSFWorkbook();
//			HSSFWorkbook wb = new HSSFWorkbook(fs.getRoot(), true);			
			sheet = wb.createSheet();
			wb.setSheetName(sheetIndex, "Rx-SCT QA Overview");
			r = null;
			c = null;
			cs = wb.createCellStyle();
			cs2 = wb.createCellStyle();
			f = wb.createFont();
			f2 = wb.createFont();
			f.setFontHeightInPoints((short) 10);
			f2.setFontHeightInPoints((short) 10);
			f2.setBold(true);
			cs.setFont(f);
			cs2.setFont(f2);
			rownum = 0;
			columnHeadings = new ArrayList<String>();
			//Thanks John C.
			try {
				pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(outputFileText)),StandardCharsets.UTF_8),true);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
	}
	
	public ExcelReport(String filename, String sheetName) {
//		NPOIFSFileSystem fs = new NPOIFSFileSystem(new File(filename));
		file = new File(filename);			
		try {
			out = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		outputFileText = filename.replace(".xls", ".txt");
		wb = new HSSFWorkbook();
//		HSSFWorkbook wb = new HSSFWorkbook(fs.getRoot(), true);			
		sheet = wb.createSheet();
		if(sheetName.length() >= 30) {
			sheetName = sheetName.substring(0, 29);
		}
		wb.setSheetName(sheetIndex, sheetName);
		r = null;
		c = null;
		cs = wb.createCellStyle();
		cs2 = wb.createCellStyle();
		f = wb.createFont();
		f2 = wb.createFont();
		f.setFontHeightInPoints((short) 10);
		f2.setFontHeightInPoints((short) 10);
		f2.setBold(true);
		cs.setFont(f);
		cs2.setFont(f2);
		rownum = 0;
		columnHeadings = new ArrayList<String>();
		//Thanks John C.
		try {
			pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(outputFileText)),StandardCharsets.UTF_8),true);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
}	
	
	public void createNewSheet(String sheetname) {
		sheet.createFreezePane(0, 1);
		sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, columnNumbers));
		
		++sheetIndex;	
		sheet = wb.createSheet();
		wb.setSheetName(sheetIndex, sheetname);		
		r = null;
		c = null;
		rownum = 0;
		outputFileText = sheetname.concat(".txt");		
		try {
			pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(outputFileText)), StandardCharsets.UTF_8), true);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void printHeader(String header) {
		String[] values = header.split("\t");
		r = sheet.createRow(rownum);
		for(int i=0; i < values.length; i++) {
			columnHeadings.add(values[i]);
			c = r.createCell(i);
			c.setCellValue(values[i]);
			c.setCellStyle(cs2);
		}		
		pw.write(header + "\n");
		pw.flush();
		rownum++;		
		columnNumbers = values.length;
	}
	
	public void printPartitionedString(String partition, String delimiter) {
		String[] values = partition.split("\n");
		if(values != null && values.length > 0) {
			printHeader(values[0], ";");
			for(int i=1; i < values.length; i++) {
				print(values[i], ";");
			}
		}
		
	}
	
	public void printHeader(String header, String delim) {
		String[] values = header.split(delim);
		r = sheet.createRow(rownum);
		for(int i=0; i < values.length; i++) {
			columnHeadings.add(values[i]);
			c = r.createCell(i);
			c.setCellValue(values[i]);
			c.setCellStyle(cs2);
		}		
		pw.write(header + "\n");
		pw.flush();
		rownum++;		
		columnNumbers = values.length;
	}	
	
	public void print(String reportRow) {
		String[] values = reportRow.split("\t");
		r = sheet.createRow(rownum);
		for(int i=0; i < values.length; i++) {
			c = r.createCell(i);
			c.setCellValue(values[i]);
			c.setCellStyle(cs);
		}		
		pw.write(reportRow + "\n");
		pw.flush();
		rownum++;		
	}
	
	public void print(String reportRow, String delim) {
		String[] values = reportRow.split(delim);
		r = sheet.createRow(rownum);
		for(int i=0; i < values.length; i++) {
			c = r.createCell(i);
			c.setCellValue(values[i]);
			c.setCellStyle(cs);
		}		
		pw.write(reportRow + "\n");
		pw.flush();
		rownum++;		
	}	
	
	public void close() {
		try {
//			fs.close();
			wb.write(out);
			out.close();
			pw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

}
