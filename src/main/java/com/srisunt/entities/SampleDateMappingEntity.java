package com.srisunt.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

import static org.springframework.data.elasticsearch.annotations.FieldType.text;

/**
 * @author Jakub Vavrik
 */
@Document(indexName = "test-index-date-mapping", type = "mapping", shards = 1, replicas = 0, refreshInterval = "-1")
public class SampleDateMappingEntity {

	@Id
	private String id;

	@Field(type = text, index = false, store = true, analyzer = "standard")
	private String message;

	@Field(type = FieldType.Date, format = DateFormat.custom, pattern = "dd.MM.yyyy hh:mm")
	private Date customFormatDate;

	@Field(type = FieldType.Date)
	private Date defaultFormatDate;

	@Field(type = FieldType.Date, format = DateFormat.basic_date)
	private Date basicFormatDate;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Date getCustomFormatDate() {
		return customFormatDate;
	}

	public void setCustomFormatDate(Date customFormatDate) {
		this.customFormatDate = customFormatDate;
	}

	public Date getDefaultFormatDate() {
		return defaultFormatDate;
	}

	public void setDefaultFormatDate(Date defaultFormatDate) {
		this.defaultFormatDate = defaultFormatDate;
	}

	public Date getBasicFormatDate() {
		return basicFormatDate;
	}

	public void setBasicFormatDate(Date basicFormatDate) {
		this.basicFormatDate = basicFormatDate;
	}
}
