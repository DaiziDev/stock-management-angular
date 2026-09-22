import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EntrepriseService } from '../services/entreprise-service';
import { EntrepriseRequest } from '../models/entreprise.model';

@Component({
  selector: 'app-entreprise-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './entreprise-form.html',
})
export class EntrepriseForm implements OnInit {
  private fb = inject(FormBuilder);
  private entrepriseService = inject(EntrepriseService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  editId: number | null = null;

  form = this.fb.group({
    nom: ['', Validators.required],
    adresse1: [''],
    adresse2: [''],
    ville: [''],
    codePostal: [''],
    pays: [''],
    mail: [''],
    numTel: [''],
  });

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editId = Number(idParam);
      const existant = this.entrepriseService.entreprises().find((e) => e.id === this.editId);
      if (existant) this.form.patchValue(existant);
    }
  }

  enregistrer() {
    if (this.form.invalid) return;
    const dto = this.form.getRawValue() as EntrepriseRequest;
    const action = this.editId
      ? this.entrepriseService.update(this.editId, dto)
      : this.entrepriseService.create(dto);
    action.subscribe(() => this.router.navigate(['/entreprises']));
  }
}
