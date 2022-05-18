package study.querydsl.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import static lombok.AccessLevel.PROTECTED;


@Getter
@MappedSuperclass
@NoArgsConstructor(access = PROTECTED)
public class BaseIdEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Override
    public String toString() {
        return "id=" + id;
    }
}


