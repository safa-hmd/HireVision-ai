import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'badgeUnlock' })
export class BadgeUnlockPipe implements PipeTransform {
  transform(badges: { unlocked: boolean }[]): number {
    return (badges || []).filter(b => b.unlocked).length;
  }
}
