package com.expirymate.document.repo;

import com.expirymate.document.model.Document;
import com.expirymate.document.model.DocumentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

	List<Document> findByUserIdOrderByExpiryDateAsc(Long userId);

	List<Document> findByUserIdAndNameContainingIgnoreCaseOrderByExpiryDateAsc(Long userId, String name);

	List<Document> findByUserIdAndCategoryOrderByExpiryDateAsc(Long userId, DocumentCategory category);

	List<Document> findByExpiryDateBetween(LocalDate from, LocalDate to);

	boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

	boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);
}
