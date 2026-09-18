package com.expirymate.document;
import com.expirymate.document.model.*;import org.junit.jupiter.api.Test;import java.time.LocalDate;import static org.junit.jupiter.api.Assertions.*;
class DocumentStatusTest{@Test void expired(){Document d=new Document();d.setExpiryDate(LocalDate.now().minusDays(1));assertEquals(DocumentStatus.EXPIRED,d.getStatus());}@Test void expiring(){Document d=new Document();d.setExpiryDate(LocalDate.now().plusDays(10));assertEquals(DocumentStatus.EXPIRING,d.getStatus());}}
