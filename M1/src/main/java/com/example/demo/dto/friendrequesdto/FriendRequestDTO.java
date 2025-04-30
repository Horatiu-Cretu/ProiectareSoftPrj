package com.example.demo.dto.friendrequesdto;

public class FriendRequestDTO {
    private Long idSender;
    private Long idReceiver;

    public Long getIdSender() {
        return idSender;
    }

    public void setIdSender(Long idSender) {
        this.idSender = idSender;
    }

    public Long getIdReceiver() {
        return idReceiver;
    }

    public void setIdReceiver(Long idReceiver) {
        this.idReceiver = idReceiver;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long idSender;
        private Long idReceiver;

        public Builder idSender(Long idSender) {
            this.idSender = idSender;
            return this;
        }

        public Builder idReceiver(Long idReceiver) {
            this.idReceiver = idReceiver;
            return this;
        }

        public FriendRequestDTO build() {
            FriendRequestDTO dto = new FriendRequestDTO();
            dto.setIdSender(this.idSender);
            dto.setIdReceiver(this.idReceiver);
            return dto;
        }
    }
}
