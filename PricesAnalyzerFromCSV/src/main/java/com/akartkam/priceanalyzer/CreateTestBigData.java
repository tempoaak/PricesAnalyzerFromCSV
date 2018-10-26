/******************************
 * Небольшая утилита для создания тестовых csv файлов
 * В папке simple созданы тестовые файлы
 * в каждом по 100000 записей 
 *******************************/
package com.akartkam.priceanalyzer;

import com.github.javafaker.Faker;

import au.com.anthonybruno.Gen;
import au.com.anthonybruno.generator.defaults.IntGenerator;
import au.com.anthonybruno.generator.defaults.StringGenerator;
import au.com.anthonybruno.settings.CsvSettings;

public class CreateTestBigData {
	public static void main(String[] args) throws Exception  {
		Faker faker = Faker.instance();
		CsvSettings settings = new CsvSettings.Builder().setDelimiter(',').build();
		System.out.println("Job started, please wait...");
		for (int i=0; i<Constants.TEST_FILES_COUNT;i++){
			Gen.start()
			 .addField("ID", ()->faker.number().randomNumber())
			 .addField("Name", ()->faker.book().genre())
			 .addField("Condition", ()->"C"+faker.number().digits(2))
			 .addField("State", ()->"S"+faker.number().digits(2))
			 .addField("Price", ()->faker.number().randomDouble(2,10, 5000))
		     .generate(Constants.FAKE_ROWS_COUNT)
		     .asCsv(settings)
		     .toFile(Constants.DEFAULT_TEST_INPUT_DIR+"\\test"+(i+1)+".csv");
		}
		System.out.println("Job's down!");
	}
}
