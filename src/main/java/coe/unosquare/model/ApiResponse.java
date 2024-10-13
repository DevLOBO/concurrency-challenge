package coe.unosquare.model;

public record ApiResponse(boolean success, String message, Object data) {
}
