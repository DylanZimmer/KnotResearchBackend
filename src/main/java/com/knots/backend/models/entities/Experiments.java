package com.knots.backend.models.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "experiments")
public class Experiments {
    // Ids come from experiments_seq one at a time (0, 1, 2, ...). Hibernate's default reserves
    // ids in blocks of 50, so ids jumped (e.g. 952 -> 1002). The database sequence must also have
    // INCREMENT BY 1 and MINVALUE 0, or Hibernate refuses to start (see the change log, Round 10).
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "experiments_seq")
    @SequenceGenerator(name = "experiments_seq", sequenceName = "experiments_seq", initialValue = 0, allocationSize = 1)
    private Long experimentId;
    private String experimentStatus;
}
