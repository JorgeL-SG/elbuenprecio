package com.tiendaabarrotes.elbuenprecio.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre no puede estar vacío.")
    @Size(min = 3, max = 200, message = "El nombre debe tener entre 3 y 200 caracteres.")
    @Column(nullable = false, length = 200)
    private String nombre;

    @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres.")
    @Column(length = 500)
    private String descripcion;

    @NotNull(message = "El precio es requerido.")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor que 0.")
    @Digits(integer = 8, fraction = 2, message = "El precio debe tener hasta 8 dígitos enteros y 2 decimales.")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @NotNull(message = "El stock es requerido.")
    @Min(value = 0, message = "El stock no puede ser negativo.")
    @Column(nullable = false)
    private Integer stock;

    @Column(name = "nombre_imagen")
    private String nombreImagen;

    @Transient
    private MultipartFile imagenFile;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne
    @JoinColumn(name = "marca_id")
    private Marca marca;

    @Column(nullable = false)
    private boolean enOferta;

    public Producto() {
        this.enOferta = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getNombreImagen() {
        return nombreImagen;
    }

    public void setNombreImagen(String nombreImagen) {
        this.nombreImagen = nombreImagen;
    }

    public MultipartFile getImagenFile() {
        return imagenFile;
    }

    public void setImagenFile(MultipartFile imagenFile) {
        this.imagenFile = imagenFile;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Marca getMarca() {
        return marca;
    }

    public void setMarca(Marca marca) {
        this.marca = marca;
    }

    public boolean isEnOferta() {
        return enOferta;
    }

    public void setEnOferta(boolean enOferta) {
        this.enOferta = enOferta;
    }

    @Override
    public String toString() {
        return "Producto{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", precio=" + precio +
                ", stock=" + stock +
                ", nombreImagen='" + nombreImagen + '\'' +
                ", categoria=" + (categoria != null ? categoria.getNombre() : "N/A") +
                ", marca=" + (marca != null ? marca.getNombre() : "N/A") +
                ", enOferta=" + enOferta +
                '}';
    }

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetallePedido> detallesPedido = new ArrayList<>();

    public List<DetallePedido> getDetallesPedido() {
        return detallesPedido;
    }

    public void setDetallesPedido(List<DetallePedido> detallesPedido) {
        this.detallesPedido = detallesPedido;
    }

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ItemCarrito> itemsCarrito = new ArrayList<>();

    public List<ItemCarrito> getItemsCarrito() {
        return itemsCarrito;
    }

    public void setItemsCarrito(List<ItemCarrito> itemsCarrito) {
        this.itemsCarrito = itemsCarrito;
    }
}