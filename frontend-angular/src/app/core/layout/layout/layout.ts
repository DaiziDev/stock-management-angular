import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Sidebar } from '../sidebar/sidebar';
import { Navbar } from '../navbar/navbar';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, Sidebar, Navbar],
  template: `
    <app-sidebar />
    <div
      class="min-h-screen pb-14 pl-[calc(var(--sbw)+18px)] pr-8 pt-[98px] transition-[padding] duration-[350ms] ease-soft max-md:pb-12 max-md:pl-0 max-md:pr-[18px] max-md:pt-[90px]"
    >
      <app-navbar />
      <div class="animate-view">
        <router-outlet />
      </div>
    </div>
  `,
})
export class Layout {}
