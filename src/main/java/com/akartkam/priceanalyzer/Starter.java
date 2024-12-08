package com.akartkam.priceanalyzer;

import java.io.File;
import java.util.Optional;

public class Starter {
	public static void main(String[] args) throws Exception  {

		String inpFolder = null;
		if (args.length>0) inpFolder = args[0];
		inpFolder = Optional.ofNullable(inpFolder).orElse(Constants.DEFAULT_INPUT_DIR);
		
		//Проверки каталога и наличие файлов 
        File fDirScan = new File(inpFolder);
        if (!fDirScan.exists() || !fDirScan.isDirectory()) {
        	System.out.println("Указан неверный каталог с файлами для обработки.("+inpFolder+")");
        	System.out.println("Конец работы.");
        	System.exit(0);
        }

        PricesAnalyzerCSV4.execute(fDirScan);
	}
}
