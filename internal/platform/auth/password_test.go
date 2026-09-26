package auth

import "testing"

func TestArgon2idHashesAndVerifiesPassword(t *testing.T) {
	t.Parallel()

	hasher := NewPasswordHasher(DefaultPasswordParams())
	encoded, err := hasher.Hash("correct horse battery staple")
	if err != nil {
		t.Fatalf("Hash() error = %v", err)
	}
	if encoded == "correct horse battery staple" || encoded == "" {
		t.Fatalf("Hash() = %q", encoded)
	}

	matched, err := hasher.Verify("correct horse battery staple", encoded)
	if err != nil || !matched {
		t.Fatalf("Verify(correct) = %v, %v", matched, err)
	}
	matched, err = hasher.Verify("wrong password", encoded)
	if err != nil || matched {
		t.Fatalf("Verify(wrong) = %v, %v", matched, err)
	}
}

func TestArgon2idRejectsMalformedEncoding(t *testing.T) {
	t.Parallel()

	hasher := NewPasswordHasher(DefaultPasswordParams())
	if _, err := hasher.Verify("password", "$argon2id$broken"); err == nil {
		t.Fatal("Verify() error = nil, want malformed encoding error")
	}
}
