/******************************
 * Программа анализа входных файлов по признаку
 * сортировки цены во входящих csv файлах. 
 * Версия 3
 * Автор: Акчурин А.К.
 * В данной версии программы используется блокирующая очередь ArrayBlockingQueue
 * Кроме основного потока, создается отдельный поток для нахождения входных файлов и 
 * помещения их в очередь, а так же потоки (в кол-ве Constants.THREADS_COUNT), которые 
 * выполняют обработку файла
 * Для координации работы программы используется CountDownLatch
 * 
 *******************************/
package com.akartkam.priceanalyzer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.core.io.FileSystemResource;

public class PricesAnalyzerCSV3 {
	public static void execute(File fDirScan)  {
        //Используем счетчик для определения того, закончили ли работу все созданные потоки
		CountDownLatch cdl = new CountDownLatch(Constants.THREADS_COUNT); 
		//Файлы хранятся в блокурующей очереди. Они помещаются туда отдельным потоком.
		//Блокировки не требуются, так как очередь сама блокирует нужные объекты
        BlockingQueue<File> queue = new ArrayBlockingQueue<File>(Constants.FQ_SIZE);
        //Set с результатами работы  (в нем всегда будет не больше MAX_OUTPUT_FILE_ROWS_COUNT=1000 элементов)
    	NavigableSet<DomainObject> set = new TreeSet<>();
    	ExecutionContext context = new ExecutionContext();
    	//Запускаем поток, который находит файлы и помещает их в очередь
	    FileProvider1 fileProv = new FileProvider1(queue, fDirScan);
	    //Засекаем время
	    long time1 = System.currentTimeMillis();
    	System.out.println("Начало работы (Версия 3)"+"(time1="+time1+")");
	    new Thread(fileProv).start();
        //Запускаем остальные потоки для обработки файлов 
        for (int i = 1; i <= Constants.THREADS_COUNT; i++) 
           new Thread(new FileProcessor1(queue, context, set, cdl)).start();
        
        try {
        	//Ждем пока все потоки не отработают
        	cdl.await();
        	//Все ок, потоки завершили работу
        	//проверяем, есть ли что у нас в результирующем map
        	if (set.isEmpty()) {
        		System.out.println("В указанном каталоге не найдено ни одного файла .csv, либо файл(ы) пусты ("+fDirScan.getName()+")");
            	System.out.println("Конец работы.");
            	System.exit(0);        		
        	}
            //Формируем выходной файл
    		FlatFileItemWriter<DomainObject> writer = FlatFileItemFactory.writer();
    		writer.open(context);
    		writer.write(new ArrayList<DomainObject>(set));
    		writer.close();
    		long time2 = System.currentTimeMillis();
    		System.out.println("Сформирован файл " + Constants.OUTPUT_FILE_NAME+"(time2="+time2+")");
    		System.out.println("uptime="+TimeUnit.MILLISECONDS.toSeconds(time2-time1)+" сек.");
    		System.out.println("Конец работы!");
        	
        } catch (Exception e) {
        	throw new IllegalStateException(e);
        }
	}
}

/*
 * Этот класс исключительно для потока, который 
 * находит файлы и помещает их в очередь.
 * 
 * 
 */
class FileProvider1 implements Runnable {
	   //Это объект-метка, который указывает на то,
	   //что все файлы изъяты из очереди потоками FileProcessor
	   //и, что поток , который получит эту метку из очереди, просто должен завершить работу
	   public static File SING = new File("");
	   private BlockingQueue<File> queue;
	   private File startDir;

	   public FileProvider1(BlockingQueue<File> queue, File startDir) {
	      this.queue = queue;
	      this.startDir = startDir;      
	   }

	   public void run()  {
	      try {
	        File[] files = startDir.listFiles((d, n) -> n.endsWith(".csv"));
	        for (File file : files) {
	        	queue.put(file);
	        	System.out.println("Помещен в очередь "+file.getName());
	        }; 
	        queue.put(SING); 
	      }
	      catch (InterruptedException e) {
	    	  throw new IllegalStateException(e);  
	      }
	   }
}

/*
 * Этот класс потока выполняет основную работу
 * В теле метода run организован цикл с проверкой.
 * Дело в том, что эти потоки, после того как закончили обрабатывать 
 * извлеченный из очереди файл, пытаются извлечь следующий файл из очереди.
 * Если файлов больше нет (file == FileProvider.SING), то поток завершает работу.
 * Таким образом ограниченным количеством потоков, можем обратаботать
 * много файлов.  
 * 
 */
class FileProcessor1 implements Runnable {
	   private BlockingQueue<File> queue;
	   private ExecutionContext context;
	   private NavigableSet<DomainObject> set;
	   private CountDownLatch cdl;
	   public FileProcessor1(BlockingQueue<File> queue, ExecutionContext context, 
			   NavigableSet<DomainObject> set, CountDownLatch cdl) {
	      this.queue = queue;
	      this.context = context;
	      this.set = set;
	      this.cdl = cdl;
	   }

	   public void run() {
	      try
	      {
	         boolean done = false;
			 FlatFileItemReader<DomainObject> reader = FlatFileItemFactory.reader();
	         while (!done) {
	            File file = queue.take();
	            if (file == FileProvider1.SING) { 
	            	queue.put(file); 
	            	done = true; 
	            	cdl.countDown();
	            } else {
	            	//Обработка файла csv
	    			DomainObject d;
					reader.setResource(new FileSystemResource(file));
	    			reader.open(context);
	    			System.out.println("Обработка файла... "+file.getName());
	    			while ((d = reader.read()) != null) {
	    				synchronized(set){
	    	    			DomainObject dobj = d;
    						//формируем список из элементов с ID как у текщуго элемента(max=20)
	    					List<DomainObject> lsById= set.stream().filter((o)->o.getID()==dobj.getID())
		                               .collect(Collectors.toList());
	    					//Если в наборе меньше 1000 записей, то просто добавляем, но следим за тем,
	    	    			//чтобы с одинаковым ID было не больше 20 (при необходимости заменяем элемент)
	    					if (set.size()<Constants.MAX_OUTPUT_FILE_ROWS_COUNT) {
		    					//если кол-во элементов с таким ID меньше 20, просто добавляем текущий элемент
								if (lsById.size()<Constants.MAX_SAME_ID_ROWS_COUNT) {
									set.add(dobj);
								} else {//если с таким ID  20 елементов и текущий элемент по цене меньше, чем
									    //имеющийся, то заменяем его
									lsById.stream()
							                    .filter((o)->o.getPrice()>dobj.getPrice())
							                    .findFirst().ifPresent((o)->{set.remove(o); set.add(dobj);});
								}
	    					} else {//Если в наборе 1000 элементов
	    						//проверяем , если текущий элемент больше по цене, чем последний в наборе, то
	    						//игнорируем его
	    						//Исключения не будет, т.к. набор не пустой
	    						if (set.last().getPrice()<d.getPrice()) continue;
		    					//если кол-во элементов с таким ID меньше 20, просто добавляем текущий элемент
	    						//а последний 1001 удаляем
								if (lsById.size()<Constants.MAX_SAME_ID_ROWS_COUNT) {
									set.add(dobj);
									set.pollLast();
								} else {//если с таким ID  20 елементов и текущий элемент по цене меньше, чем
									    //имеющийся, то заменяем его
									lsById.stream()
							                    .filter((o)->o.getPrice()>dobj.getPrice())
							                    .findFirst().ifPresent((o)->{set.remove(o); set.add(dobj);});
								}	    						
	    						
	    					}
	    				}
	    			}
	    			reader.close();
	    			System.out.println("Завершение обработка файла  "+file.getName());
	            };
	         }
	        
	      }
	      catch (Exception e) {
	    	  throw new IllegalStateException(e);
	      }
	   }

}

