package com.codegym.controller;

import com.codegym.model.account.Account;
import com.codegym.model.account.Privacy;
import com.codegym.model.comment.Comment;
import com.codegym.model.dto.StatusDto;
import com.codegym.model.dto.StatusDtoComment;
import com.codegym.model.image.ImageStatus;
import com.codegym.model.like.LikeStatus;
import com.codegym.model.status.Status;
import com.codegym.model.status.dto.StatusDTO;
import com.codegym.service.comment.ICommentService;
import com.codegym.service.imageStatus.IStatusImageService;
import com.codegym.service.likeStatus.ILikeStatusService;
import com.codegym.service.notification.INotificationService;
import com.codegym.service.status.IStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/status")
public class StatusController {
    @Autowired
    private IStatusService statusService;

    @Autowired
    private IStatusImageService imageService;

    @Autowired
    private ICommentService commentService;

    @Autowired
    private ILikeStatusService likeStatusService;

    @Autowired
    private INotificationService notificationService;

    @GetMapping("/page/{pageId}")
    public ResponseEntity<?> getAllStatusByPageId(@PathVariable Long pageId, @RequestParam int size) {
        Iterable<Status> statuses = statusService.findAllByPageId(pageId, size);
        List<StatusDTO> statusDTOs = new ArrayList<>();
        for (Status x : statuses) {
            List<LikeStatus> likeStatuses = (List<LikeStatus>) likeStatusService.findAllByStatusId(x.getId());
            StatusDTO statusDTO = new StatusDTO(x.getId(), x.getContent(), x.getAccount(), x.getPrivacy(),
                    x.getImageStatuses(), x.getTime(), x.getPage(), likeStatuses);
            statusDTOs.add(statusDTO);
        }
        return new ResponseEntity<>(statusDTOs, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Status> findByIdStatus(@PathVariable Long id) {
        Optional<Status> statusOptional = statusService.findById(id);
        if (!statusOptional.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(statusOptional.get(), HttpStatus.OK);
    }
    @PostMapping
    public ResponseEntity<Status> createStatus(@RequestBody StatusDto statusDto) {
        ImageStatus imageStatus = new ImageStatus();
        imageStatus.setUrl(statusDto.getUrl());
        imageService.save(imageStatus);
        Status status = new Status();
        if (statusDto.getPage() != null){
            status.setPage(statusDto.getPage());
        }
        Set<ImageStatus> imageStatusSet = new HashSet<>();
        imageStatusSet.add(imageService.findByUrl(statusDto.getUrl()).get());
        status.setContent(statusDto.getContent());
        status.setAccount(statusDto.getAccount());
        status.setPrivacy(statusDto.getPrivacy());
        status.setImageStatuses(imageStatusSet);
        status.setPostedTime(new Date());
        Status saved = statusService.save(status);
        notificationService.saveStatusNotification(saved);
        return new ResponseEntity<>(saved, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Status> editStatus(@RequestBody Status status, @PathVariable Long id) {
        Optional<Status> statusOptional = statusService.findById(id);
        if (!statusOptional.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        status.setId(statusOptional.get().getId());
        return new ResponseEntity<>(statusService.save(status), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Status> deleteStatus(@PathVariable Long id) {
        Optional<Status> statusOptional = statusService.findById(id);
        if (!statusOptional.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        notificationService.deleteStatusNotification(statusOptional.get());

        statusService.delete(id);
        return new ResponseEntity<>(statusOptional.get(), HttpStatus.NO_CONTENT);
    }

    // API cho khách khi xem trang cá nhân của 1 người
    @GetMapping("/public/{id}")
    public ResponseEntity<Iterable<Status>> getAllPublicStatus(@PathVariable("id") Long id,
                                                               @RequestParam("size") Long size) {
        Iterable<Status> statuses = statusService.findAllPublicStatusByMyselfPagination(id, size);
        if (!statuses.iterator().hasNext()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(statuses, HttpStatus.OK);
    }

    // API cho bạn bè khi xem trang cá nhân
    @GetMapping("/friends/{id}")
    public ResponseEntity<Iterable<Status>> getAllFriendStatus(@PathVariable("id") Long id,
                                                               @RequestParam("size") Long size) {
        Iterable<Status> friendStatuses = statusService.findAllNonPrivateStatusByMySelfPagination(id, size);
        if (!friendStatuses.iterator().hasNext()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(friendStatuses, HttpStatus.OK);
    }

    // API cho bảng tin của cá nhân
    @GetMapping("/newsfeed/{id}")
    public ResponseEntity<?> getNewsFeed(@PathVariable("id") Long id,
                                         @RequestParam("size") Long size) {
        List<Status> newsFeed = (List<Status>) statusService.findAllStatusInNewsFeedPagination(id, size);
        List<StatusDtoComment> statusDtoCommentList = new ArrayList<>();
        for (Status x : newsFeed) {
            StatusDtoComment statusDtoComment = new StatusDtoComment();
            Page<Comment> commentList;
            commentList = commentService.findAllByStatusIdPagination(x.getId(), 3L);
            List<LikeStatus> likeStatuses;
            likeStatuses = (List<LikeStatus>) likeStatusService.findAllByStatusId(x.getId());
            statusDtoComment.setLikeStatuses(likeStatuses);
            statusDtoComment.setComments(commentList.getContent());
            statusDtoComment.setContent(x.getContent());
            statusDtoComment.setImageStatuses(x.getImageStatuses());
            statusDtoComment.setAccount(x.getAccount());
            statusDtoComment.setPrivacy(x.getPrivacy());
            statusDtoComment.setId(x.getId());
            statusDtoComment.setTime(x.getTime());
            statusDtoComment.setPostedTime(x.getPostedTime());
            statusDtoCommentList.add(statusDtoComment);
        }
        return new ResponseEntity<>(statusDtoCommentList, HttpStatus.OK);
    }

    // API cho cá nhân xem chính trang của mình
    @GetMapping("/account/{id}")
    public ResponseEntity<Iterable<Status>> getStatusByAccountId(@PathVariable Long id,
                                                                 @RequestParam Long size) {
        return new ResponseEntity<>(statusService.findAllByAccountIdPagination(id, size), HttpStatus.OK);
    }
}
