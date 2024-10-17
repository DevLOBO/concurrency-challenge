package coe.unosquare.model;

public record OrderStats(Integer buyRequests, Integer sellRequests, Integer buyProcessed, Integer sellProcessed,
		Integer buyCompleted, Integer sellCompleted) {

}
