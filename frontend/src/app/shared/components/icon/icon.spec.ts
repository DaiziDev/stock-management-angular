import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AppIcon } from './icon';

@Component({
  selector: 'app-icon-host',
  imports: [AppIcon],
  template: `<app-icon name="menu" /><app-icon name="shopping-cart" [size]="20" />`,
})
class IconHost {}

describe('AppIcon', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IconHost],
    }).compileComponents();
  });

  it('renders the SVG with the icon paths (sanitizer preserves SVG)', () => {
    const fixture = TestBed.createComponent(IconHost);
    fixture.detectChanges();
    const svgs = fixture.nativeElement.querySelectorAll('svg.shrink-0');

    expect(svgs.length).toBe(2);
    // Le chemin SVG doit être présent après sanitization d'Angular.
    expect(svgs[0].querySelector('line')).toBeTruthy();
    expect(svgs[1].querySelector('circle')).toBeTruthy();
  });

  it('applies the requested size to the svg', () => {
    const fixture = TestBed.createComponent(IconHost);
    fixture.detectChanges();
    const svgs = fixture.nativeElement.querySelectorAll('svg.shrink-0');

    expect(svgs[0].getAttribute('width')).toBe('17');
    expect(svgs[1].getAttribute('width')).toBe('20');
  });
});
