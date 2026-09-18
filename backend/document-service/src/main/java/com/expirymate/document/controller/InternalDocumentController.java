package com.expirymate.document.controller;

import com.expirymate.document.model.Document;
import com.expirymate.document.repo.DocumentRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/internal/documents")
public class InternalDocumentController {

	private final DocumentRepository repository;

	public InternalDocumentController(DocumentRepository repository) {
		this.repository = repository;
	}

	@GetMapping("/expiring")
	public List<Document> expiring(@RequestParam(defaultValue = "30") int days) {
		return repository.findByExpiryDateBetween(LocalDate.now(), LocalDate.now().plusDays(days));
	}
}
