package com.akartkam.priceanalyzer;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.core.io.FileSystemResource;

public class FlatFileItemFactory {


	public static FlatFileItemReader<DomainObject> reader(File file)
	{
	    FlatFileItemReader<DomainObject> reader = new FlatFileItemReader<>();
	    reader.setResource(new FileSystemResource(file));
	    reader.setLinesToSkip(1);  
	    reader.setLineMapper(new DefaultLineMapper<DomainObject>() {
	        {
	            setLineTokenizer(new DelimitedLineTokenizer() {
	                {
	                    setNames(Constants.FIELD_NAMES);
	                }
	            });
	            setFieldSetMapper(new BeanWrapperFieldSetMapper<DomainObject>() {
	                {
	                    setTargetType(DomainObject.class);
	                }
	            });
	        }
	    });
	    return reader;
	}

	public static FlatFileItemWriter<DomainObject> writer() {
		FlatFileItemWriter<DomainObject> writer = new FlatFileItemWriter<>();
		writer.setResource(new FileSystemResource(Constants.OUTPUT_FILE_NAME));
		DelimitedLineAggregator<DomainObject> aggr = new DelimitedLineAggregator<>();
		BeanWrapperFieldExtractor<DomainObject> extr = new BeanWrapperFieldExtractor<>();
		extr.setNames(Constants.FIELD_NAMES);
		aggr.setFieldExtractor(extr);
		writer.setHeaderCallback(new FlatFileHeaderCallback() {
			@Override
			public void writeHeader(Writer writer) throws IOException {
				writer.write(String.join(",",Constants.FIELD_NAMES));
				
			}
		});
		writer.setLineAggregator(aggr);
		return writer;
	}

}
