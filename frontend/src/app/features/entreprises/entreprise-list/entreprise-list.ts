import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { EntrepriseService } from '../services/entreprise-service';

@Component({
  selector: 'app-entreprise-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './entreprise-list.html'
})
export class EntrepriseList implements OnInit {
    ngOnInit(): void {
        this.entrepriseService.loadAll();
    }

    private entrepriseService = inject(EntrepriseService);
    entreprises = this.entrepriseService.entreprises;

    supprimer(id: number) {
        this.entrepriseService.delete(id).subscribe(() => this.entrepriseService.loadAll())
    }
}
