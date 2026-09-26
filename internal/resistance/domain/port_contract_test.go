package domain

import "testing"

func TestProfilePatchPreservesOmittedFields(t *testing.T) {
	original := Profile{UserID: 1, Address: "old", Gold: 99, HighScore: 10, Energy: 90, Scenario: 2, Head: 3, Body: 4, Arm: 5, Health: 100, Attack: 10}
	energy := 50
	updated, err := (ProfilePatch{Energy: &energy}).Apply(original)
	if err != nil {
		t.Fatalf("Apply() error = %v", err)
	}
	if updated.Energy != 50 || updated.Address != "old" || updated.Gold != 99 || updated.HighScore != 10 {
		t.Fatalf("updated = %#v", updated)
	}
}

func TestCanonicalFriendPair(t *testing.T) {
	low, high, err := CanonicalFriendPair(9, 3)
	if err != nil || low != 3 || high != 9 {
		t.Fatalf("pair = %d %d %v", low, high, err)
	}
	if _, _, err := CanonicalFriendPair(3, 3); err == nil {
		t.Fatal("self friendship accepted")
	}
}

func TestAdjustedRateContract(t *testing.T) {
	if got := AdjustedRate(100, 0, 0); got != 100 {
		t.Fatalf("zero usage rate = %d", got)
	}
	if got := AdjustedRate(100, 25, 100); got != 102 {
		t.Fatalf("weighted rate = %d", got)
	}
	if got := AdjustedRate(0, 0, 1); got != 1 {
		t.Fatalf("minimum rate = %d", got)
	}
}
