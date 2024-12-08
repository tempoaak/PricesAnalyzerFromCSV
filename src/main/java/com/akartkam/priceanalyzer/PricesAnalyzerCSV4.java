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
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.core.io.FileSystemResource;

import static com.akartkam.priceanalyzer.FileProvider1.SING;

public class PricesAnalyzerCSV4 {
	//Constants.THREADS_COUNT
	//Файлы хранятся в блокурующей очереди. Они помещаются туда отдельным потоком.
	//Блокировки не требуются, так как очередь сама блокирует нужные объекты
	final static BlockingQueue<File> queue = new ArrayBlockingQueue<File>(Constants.FQ_SIZE);
	//Set с результатами работы  (в нем всегда будет не больше MAX_OUTPUT_FILE_ROWS_COUNT=1000 элементов)
	final static NavigableSet<DomainObject> set = new TreeSet<>();
	final static ExecutionContext context = new ExecutionContext();
	final static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public static void execute(File fDirScan) throws Exception {
		//Запускаем поток, который находит файлы и помещает их в очередь
		//Засекаем время
		long time1 = System.currentTimeMillis();
		System.out.println("Начало работы (Версия 3)" + "(time1=" + time1 + ")");
		CompletableFuture<Integer> fileProducer = CompletableFuture.supplyAsync(() ->
				{
					int i = 0;
					try {
						File[] files = fDirScan.listFiles((d, n) -> n.endsWith(".csv"));
						System.out.println("Всего в папке найдено файлов: " + files.length);
						for (File file : files) {
							queue.put(file);
							System.out.println("Помещен в очередь " + file.getName());
							i++;
						}
						;
						queue.put(SING);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					return i;
				}
		);
		List<CompletableFuture<?>> futureList = new ArrayList<>();
		//Запускаем остальные потоки для обработки файлов
		for (int i = 1; i <= Constants.THREADS_COUNT; i++) {
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> PricesAnalyzerCSV4.processFile() );
			futureList.add(future);
		}
		System.out.println("Всего потоков: " + futureList.size());
		CompletableFuture.allOf(futureList.toArray(new CompletableFuture<?>[Constants.THREADS_COUNT])).join();

		if (set.isEmpty()) {
			System.out.println("В указанном каталоге не найдено ни одного файла .csv, либо файл(ы) пусты (" + fDirScan.getName() + ")");
			System.out.println("Конец работы.");
			System.exit(0);
		}
		//Формируем выходной файл
		FlatFileItemWriter<DomainObject> writer = FlatFileItemFactory.writer();
		writer.open(context);
		writer.write(new ArrayList<DomainObject>(set));
		writer.close();

		long time2 = System.currentTimeMillis();
		System.out.println("Сформирован файл " + Constants.OUTPUT_FILE_NAME + "(time2=" + time2 + ")");

		System.out.println("Всего помещено в очередь файлов: " + fileProducer.join());
		System.out.println("uptime=" + TimeUnit.MILLISECONDS.toSeconds(time2 - time1) + " сек.");
		System.out.println("Конец работы!");

	}

	private static void processFile()
	{
		FlatFileItemReader<DomainObject> reader = FlatFileItemFactory.reader();
		while (true) {
			File file = null;
			try {
				file = queue.take();
				if (file == SING) {
					queue.put(file);
					break;
				}
			} catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
			}
			DomainObject d;
			reader.setResource(new FileSystemResource(file));
			reader.open(context);
			System.out.println("Обработка файла... " + file.getName());
			try {
				while (((d = reader.read()) != null)) {
					lock.writeLock().lock(); // Захват блокировки для записи
					try {
						DomainObject dobj = d;
						//формируем список из элементов с ID как у текщуго элемента(max=20)
						List<DomainObject> lsById = set.stream().filter((o) -> o.getID() == dobj.getID())
								.collect(Collectors.toList());
						if (lsById.size() < Constants.MAX_SAME_ID_ROWS_COUNT) {
							set.add(dobj);
						} else {
							lsById.stream()
									.filter((o) -> o.getPrice() > dobj.getPrice())
									.findFirst().ifPresent((o) -> {
										set.remove(o);
										set.add(dobj);
									});
						}

						if (set.size() > Constants.MAX_OUTPUT_FILE_ROWS_COUNT) {
							set.pollLast();
						}
					} finally {
						lock.writeLock().unlock(); // Освобождение блокировки
					}
				}
			} catch (Exception e) {
				System.out.println("Ошибка обработки файла... " + file.getName());
				continue;
			}
			reader.close();
			System.out.println("Завершение обработки файла  " + file.getName());

		}

	}
}


