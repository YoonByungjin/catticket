import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BROWSER_STORAGE } from '../storage/safe-storage';
import { NOTIFICATION_DURATION_MS, NotificationService } from './notification.service';

describe('NotificationService', () => {
  it('opens a snack bar with the dictionary text of the current language', () => {
    const open = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        { provide: MatSnackBar, useValue: { open } },
        { provide: BROWSER_STORAGE, useValue: null },
      ],
    });

    TestBed.inject(NotificationService).show('notify.loggedOut');

    expect(open).toHaveBeenCalledWith('로그아웃했습니다', undefined, {
      duration: NOTIFICATION_DURATION_MS,
    });
  });

  it('shows the message in a real snack bar', async () => {
    TestBed.configureTestingModule({ providers: [{ provide: BROWSER_STORAGE, useValue: null }] });
    TestBed.inject(NotificationService).show('notify.sessionExpired');
    await new Promise((resolve) => setTimeout(resolve));
    expect(document.body.textContent).toContain('일정 시간 조작이 없어 로그아웃되었습니다');
  });
});
