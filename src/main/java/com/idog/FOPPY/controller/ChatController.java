package com.idog.FOPPY.controller;

import com.idog.FOPPY.dto.ResponseDTO;
import com.idog.FOPPY.dto.chat.*;
import com.idog.FOPPY.service.ChatRoomService;
import com.idog.FOPPY.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = {"http://localhost:3000"})
@Tag(name = "chat", description = "채팅 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate template;
    private final ChatService chatService;
    private final ChatRoomService chatRoomService;

    @MessageMapping("/send")
    public void send(@Payload ChatMessageDTO.Send message) {
        ChatMessageDTO.Response response = chatService.sendMessage(message);
        template.convertAndSend("/sub/room/" + message.getRoomId(), message);  // /sub/room/{roomId} 구독자에게 메시지 전달
//        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseDTO<>(true, "Success", response));
    }

    @PostMapping("/room")
    @Operation(summary = "채팅방 생성 1 (memberId, dogId)")
    public ResponseEntity<ResponseDTO<ChatRoomDTO.JoinResponse>> join1(@RequestBody ChatRoomDTO.JoinRequest1 requestDto) {
        try {
            ChatRoomDTO.JoinResponse joinResponse = chatRoomService.join1(requestDto);

            ResponseDTO<ChatRoomDTO.JoinResponse> response = new ResponseDTO<>();
            response.setStatus(true);
            response.setMessage("Success");
            response.setData(joinResponse);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage()));
        }
    }

    @PostMapping("/room2")
    @Operation(summary = "채팅방 생성 2 (member1Id, member2Id)")
    public ResponseEntity<ResponseDTO<ChatRoomDTO.JoinResponse>> join2(@RequestBody ChatRoomDTO.JoinRequest2 request) {
        try {
            ChatRoomDTO.JoinResponse joinResponse = chatRoomService.join2(request);

            ResponseDTO<ChatRoomDTO.JoinResponse> response = new ResponseDTO<>();
            response.setStatus(true);
            response.setMessage("Success");
            response.setData(joinResponse);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage()));
        }
    }

    @GetMapping("/rooms")
    @Operation(summary = "채팅방 목록 조회")
    public ResponseEntity<ResponseDTO<List<ChatRoomDTO.Response>>> getChatRoomList() {
        try {
            List<ChatRoomDTO.Response> chatRooms = chatRoomService.getChatRoomList();

            ResponseDTO<List<ChatRoomDTO.Response>> response = new ResponseDTO<>();
            response.setStatus(true);
            response.setMessage("Success");
            response.setData(chatRooms);

            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage()));
        }
    }

    @GetMapping("/room/{roomId}")
    @Operation(summary = "채팅방 상세 조회")
    public ResponseEntity<ResponseDTO<ChatRoomDTO.Detail>> getChatRoomDetail(@PathVariable Long roomId) {
        try {
            ChatRoomDTO.Detail chatRoom = chatRoomService.getChatRoomDetail(roomId);

            ResponseDTO<ChatRoomDTO.Detail> response = new ResponseDTO<>();
            response.setStatus(true);
            response.setMessage("Success");
            response.setData(chatRoom);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage()));
        }
    }

    @DeleteMapping("/room/{roomId}")
    @Operation(summary = "채팅방 삭제")
    public ResponseEntity<ResponseDTO<Void>> deleteChatRoom(@PathVariable Long roomId) {
        try {
            chatRoomService.deleteChatRoom(roomId);

            ResponseDTO<Void> response = new ResponseDTO<>();
            response.setStatus(true);
            response.setMessage("Success");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage()));
        }
    }
}
