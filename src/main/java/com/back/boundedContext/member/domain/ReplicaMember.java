package com.back.boundedContext.member.domain;

import com.back.shared.member.domain.BaseMember;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class ReplicaMember extends BaseMember {
    @Id
    private int id;
    private LocalDateTime createDate;
    private LocalDateTime modifyDate;

    public ReplicaMember(String username, String password, String nickname) {
        super(username, password, nickname);
    }
}