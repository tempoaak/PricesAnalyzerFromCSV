package com.akartkam.priceanalyzer;

public interface Constants {
	public static final String DEFAULT_VERSION = "3";
    //по умолчанию, если при запуске не указана папка с файлами, используем текущую
	public static final String DEFAULT_INPUT_DIR = System.getProperty("user.dir");
	//public static final String DEFAULT_INPUT_DIR = "..\\samples\\";
	//Количество fake строк в каждом тестовом файле
	public static final int FAKE_ROWS_COUNT = 100000;
	//Количество тестовых файлов
	public static final int TEST_FILES_COUNT = 100;
	//папка для тестовых csv файлов
	public static final String DEFAULT_TEST_INPUT_DIR = "..\\samples\\";
	//Наименования полей сущности )
	public static final String[] FIELD_NAMES = {"ID","Name","Condition","State","Price"};
	//Наименование выходного файла по умолчанию
	public static final String OUTPUT_FILE_NAME = "output.csv";
	//Максимальное количество строк в выходном файле (+ заголовок)
	public static final int MAX_OUTPUT_FILE_ROWS_COUNT = 1000;
	//Максимальное количество строк с одинаковым ID 
	public static final int MAX_SAME_ID_ROWS_COUNT = 20;
	
	
	//Размер блокирующей очереди с файлами
	public final int FQ_SIZE = 20;
    //Количество потоков для обработки файлов
	public final int THREADS_COUNT = 10;
	
	
}
