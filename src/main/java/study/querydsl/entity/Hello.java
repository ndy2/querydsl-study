package study.querydsl.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
public class Hello extends BaseIdEntity{
}
