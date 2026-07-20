import { BadgeUnlockPipe } from './badge-unlock.pipe';

describe('BadgeUnlockPipe', () => {
  let pipe: BadgeUnlockPipe;

  beforeEach(() => {
    pipe = new BadgeUnlockPipe();
  });

  it('should create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('should count only the unlocked badges', () => {
    const badges = [{ unlocked: true }, { unlocked: false }, { unlocked: true }, { unlocked: true }];
    expect(pipe.transform(badges)).toBe(3);
  });

  it('should return 0 when no badge is unlocked', () => {
    const badges = [{ unlocked: false }, { unlocked: false }];
    expect(pipe.transform(badges)).toBe(0);
  });

  it('should return 0 for an empty array', () => {
    expect(pipe.transform([])).toBe(0);
  });

  it('should return 0 for null/undefined input', () => {
    expect(pipe.transform(null as any)).toBe(0);
    expect(pipe.transform(undefined as any)).toBe(0);
  });
});
