import { Component, AfterViewInit } from '@angular/core';

declare const lucide: any;

@Component({
  selector: 'app-menu',
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.css']
})
export class MenuComponent implements AfterViewInit {
  ngAfterViewInit(): void {
    lucide.createIcons();
  }
}
