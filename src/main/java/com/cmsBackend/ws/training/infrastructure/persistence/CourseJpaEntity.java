package com.cmsBackend.ws.training.infrastructure.persistence;

import com.cmsBackend.ws.training.domain.CourseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "courses", indexes = {
    @Index(name = "idx_courses_status_name", columnList = "status,name")
})
public class CourseJpaEntity {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false)
    private int durationHours;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal listPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseStatus status;

    @Version
    private long version;

    protected CourseJpaEntity() {}

    public CourseJpaEntity(
            UUID id, String code, String name, String category, int durationHours, BigDecimal listPrice,
            CourseStatus status) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.category = category;
        this.durationHours = durationHours;
        this.listPrice = listPrice;
        this.status = status;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public int getDurationHours() { return durationHours; }
    public BigDecimal getListPrice() { return listPrice; }
    public CourseStatus getStatus() { return status; }
    public long getVersion() { return version; }

    public void update(String name, String category, int durationHours, BigDecimal listPrice, CourseStatus status) {
        this.name = name;
        this.category = category;
        this.durationHours = durationHours;
        this.listPrice = listPrice;
        this.status = status;
    }
}
