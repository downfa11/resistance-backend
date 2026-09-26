package notifications

import (
	"context"
	"errors"
	"testing"
	"time"
)

func TestServiceValidatesNotificationInput(t *testing.T) {
	t.Parallel()

	service := NewService(&memoryRepository{})
	for _, request := range []CreateRequest{
		{UserID: 0, Source: SourceSystem, Title: "Title", Body: "Body"},
		{UserID: 1, Source: "UNKNOWN", Title: "Title", Body: "Body"},
		{UserID: 1, Source: SourceSystem, Title: "", Body: "Body"},
		{UserID: 1, Source: SourceSystem, Title: "Title", Body: ""},
	} {
		if _, err := service.Create(context.Background(), request); !errors.Is(err, ErrInvalidInput) {
			t.Fatalf("Create(%#v) error = %v, want ErrInvalidInput", request, err)
		}
	}
}

type memoryRepository struct {
	created *Notification
}

func (m *memoryRepository) Create(_ context.Context, item *Notification) error {
	copy := *item
	copy.ID = 1
	item.ID = 1
	m.created = &copy
	return nil
}
func (*memoryRepository) List(context.Context, int64, int, int64, time.Time) ([]*Notification, error) {
	return nil, nil
}
func (*memoryRepository) Find(context.Context, int64, int64, time.Time) (*Notification, error) {
	return nil, ErrNotFound
}
func (*memoryRepository) MarkRead(context.Context, int64, int64, time.Time) (*Notification, error) {
	return nil, ErrNotFound
}
func (*memoryRepository) MarkAllRead(context.Context, int64, time.Time) (int64, error) {
	return 0, nil
}
