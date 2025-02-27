package RunningMate.backend.domain.running.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RunningGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;

    @Column(nullable = false)
    private String groupTitle;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Long targetDistance;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Column(nullable = false)
    private Integer currentParticipants;

    @Enumerated(EnumType.STRING)
    private GroupTag groupTag;

    @Column(nullable = false)
    private Boolean activate;

    @OneToMany(mappedBy = "group")
    private List<LeaderBoard> leaderBoardList;

    public void deactivate(){
        this.activate = false;
    }
    public boolean participateGroup(){
        if(this.currentParticipants >= this.maxParticipants)
            return false;
        else {
            this.currentParticipants += 1;
            return true;
        }
    }

    public boolean leaveGroup(){
        if(this.currentParticipants <= 0)
            return false;
        else{
            this.currentParticipants -= 1;
            return true;
        }
    }
}
